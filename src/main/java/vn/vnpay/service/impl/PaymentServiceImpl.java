package vn.vnpay.service.impl;

import com.google.gson.Gson;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import vn.vnpay.common.enums.PaymentResponseCode;
import vn.vnpay.common.exception.GlobalExceptionHandler;
import vn.vnpay.common.exception.PaymentException;
import vn.vnpay.common.response.BaseResponse;
import vn.vnpay.common.response.PaymentHttpResponse;
import vn.vnpay.common.response.PaymentResponse;
import vn.vnpay.config.bankcode.XmlBankValidator;
import vn.vnpay.dto.payment.request.PaymentRequestDTO;
import vn.vnpay.enums.MessageType;
import vn.vnpay.service.PaymentService;
import vn.vnpay.service.RabbitMQService;
import vn.vnpay.service.RedisService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final RedisService redisService;
    private final GlobalExceptionHandler exceptionHandler;
    private final RabbitMQService rabbitMQService;
    private final XmlBankValidator xmlBankValidator;
    private final Gson gson;
    private static final String PHONE_REGEX = "^(03|05|07|08|09)[0-9]{8}$";

    public PaymentServiceImpl(RedisService redisService, GlobalExceptionHandler exceptionHandler,
                              RabbitMQService rabbitMQService, XmlBankValidator xmlBankValidator,
                              Gson gson) {
        this.redisService = redisService;
        this.exceptionHandler = exceptionHandler;
        this.rabbitMQService = rabbitMQService;
        this.xmlBankValidator = xmlBankValidator;
        this.gson = gson;
    }

    @Override
    public void createTokenKey(ChannelHandlerContext ctx, HttpRequest request) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            Base64.Encoder base64Encoder = Base64.getUrlEncoder();
            byte[] randomBytes = new byte[24];
            secureRandom.nextBytes(randomBytes);
            String tokenKey = base64Encoder.encodeToString(randomBytes);
            log.info("tokenKey: {} ", tokenKey);
            String jsonResponse = gson.toJson(BaseResponse.success(tokenKey));
            // Gửi phản hồi về client và đóng kết nối sau khi gửi
            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(jsonResponse)).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            exceptionHandler.handleException(ctx, e, PaymentResponseCode.UNKNOWN_ERROR.getCode(), null);
        }
    }

    @Override
    public void createPayment(ChannelHandlerContext ctx, FullHttpRequest request) {
        String privateKey = null;
        try {
            String requestBody = request.content().toString(CharsetUtil.UTF_8);

            PaymentRequestDTO paymentRequest = gson.fromJson(requestBody, PaymentRequestDTO.class);
            privateKey = paymentRequest.getPrivateKey();
            log.info("Begin create payment: {}", gson.toJson(paymentRequest));

            //validate request
            validateRequest(paymentRequest);

            //push data to rabbitMQ
            pushDataToRabbitMQ(ctx, paymentRequest);


            ctx.writeAndFlush(PaymentHttpResponse
                            .responseSuccess(gson.toJson(PaymentResponse.success(paymentRequest.getPrivateKey(), paymentRequest.getAddValue()))))
                    .addListener(ChannelFutureListener.CLOSE);
        } catch (PaymentException pe) {
            exceptionHandler.handleException(ctx, pe, PaymentResponseCode.FIELD_ERROR.getCode(), privateKey);
        } catch (Exception e) {
            exceptionHandler.handleException(ctx, e, PaymentResponseCode.UNKNOWN_ERROR.getCode(), privateKey);
        }
    }

    private void pushDataToRabbitMQ(ChannelHandlerContext ctx, PaymentRequestDTO request) throws Exception {

        String message = gson.toJson(request);
        String responseMessage = rabbitMQService.sendMessageWithReply(message, "nettyRoutingKey");
        log.info("phản hồi từ consumer: {}", responseMessage);
    }

    private void validateRequest(PaymentRequestDTO request) throws PaymentException {

        //validate token && save token in redis
        if (StringUtils.isBlank(request.getPrivateKey())) {
            throw new PaymentException("tokenKey không được để trống hoặc có khoảng trắng");
        }
        if (redisService.existDataFromRedis(request.getTokenKey())) {
            throw new PaymentException("tokenKey đã trùng lặp trong ngày");
        }
        redisService.saveDataToRedis(request.getTokenKey(), gson.toJson(request));


        //validate phoneNumber
        if (StringUtils.isBlank(request.getMobile())) {
            throw new PaymentException("mobile - Số điện thoại của khách hàng không đuợc để trống.");
        }
        Pattern pattern = Pattern.compile(PHONE_REGEX);

        Matcher matcher = pattern.matcher(request.getMobile());

        if (!matcher.matches()) {
            throw new PaymentException("mobile - Sai định dạng số điện thoại của khách hàng.");
        }

        //validate BankCode && PrivateKey
        if (StringUtils.isBlank(request.getBankCode())) {
            throw new PaymentException("bankCode không được trống.");
        }
        if (StringUtils.isBlank(request.getPrivateKey())) {
            throw new PaymentException("privateKey không được trống.");
        }
        if (!xmlBankValidator.isValidBank(request.getBankCode(), request.getPrivateKey())) {
            throw new PaymentException("bankCode and privateKey không cùng 1 ngân hàng.");
        }

        // So sánh hai giá trị debitAmount(số tiền thanh toán) và realAmount(số tiền sau khuyến mại)
        int comparisonResult = request.getDebitAmount().compareTo(request.getRealAmount());

        if (comparisonResult < 0) { // debitAmount nhỏ hơn realAmount
            throw new PaymentException("Số tiền thanh toán phải hơn số tiền sau khuyến mại.");
        } else if (comparisonResult == 0) { // debitAmount bằng realAmount
            if (!StringUtils.isEmpty(request.getPromotionCode())) {
                throw new PaymentException("Mã Voucher phải bằng null hoặc rỗng khi không có khuyến mãi.");
            }
        } else { // debitAmount lớn hơn realAmount
            if (StringUtils.isEmpty(request.getPromotionCode())) {
                throw new PaymentException("Error code 01: Không có mã voucher, mặc dù số tiền đã giảm.");
            }
        }

        if (StringUtils.isEmpty(request.getPayDate())) {
            throw new PaymentException("payDate không được trống");
        }
        if (!isValidPayDate(request.getPayDate())) {
            throw new PaymentException("payDate không đúng định dạng");
        }

        if (StringUtils.isBlank(request.getApiId())) {
            throw new PaymentException("apiId không được trống hoặc không có khoảng trắng");
        }

        if (StringUtils.isBlank(request.getOderCode())) {
            throw new PaymentException("oderCode không được trống hoặc không có khoảng trắng");
        }

        if (StringUtils.isBlank(request.getRespCode())) {
            throw new PaymentException("respCode không được trống hoặc không có khoảng trắng");
        }

        if (StringUtils.isEmpty(request.getRespDesc())) {
            throw new PaymentException("respDesc không được trống");
        }

        if (StringUtils.isBlank(request.getTraceTransfer())) {
            throw new PaymentException("traceTransfer không được trống");
        }

        if (StringUtils.isEmpty(request.getMessageType())) {
            throw new PaymentException("messageType không được trống");
        }
        if (!request.getMessageType().equals(MessageType.SUCCESS.getCode())) {
            throw new PaymentException("Giá trị của messageType không chính xác");
        }

        if (StringUtils.isBlank(request.getAddValue().getPayMethod())) {
            throw new PaymentException("payMethod không được trống");
        }
        if (request.getAddValue().getPayMethodMMS() == null) {
            throw new PaymentException("payMethodMMS không được trống");
        }
    }

    private boolean isValidPayDate(String payDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        try {
            LocalDateTime.parse(payDate, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}
