package vn.vnpay.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import vn.vnpay.dto.payment.response.ConsumerResponse;
import vn.vnpay.enums.MessageType;
import vn.vnpay.enums.QueueName;
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
    private final ObjectMapper objectMapper;

    private static final String PHONE_REGEX = "^(03|05|07|08|09)[0-9]{8}$";

    public PaymentServiceImpl(RedisService redisService, GlobalExceptionHandler exceptionHandler,
                              RabbitMQService rabbitMQService, XmlBankValidator xmlBankValidator,
                              Gson gson, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.exceptionHandler = exceptionHandler;
        this.rabbitMQService = rabbitMQService;
        this.xmlBankValidator = xmlBankValidator;
        this.gson = gson;
        this.objectMapper = objectMapper;
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
            sendDataToRabbitMQAndReply(paymentRequest);

            ctx.writeAndFlush(PaymentHttpResponse
                            .responseSuccess(gson.toJson(PaymentResponse.success(paymentRequest.getPrivateKey(), paymentRequest.getAddValue()))))
                    .addListener(ChannelFutureListener.CLOSE);
        } catch (PaymentException pe) {
            exceptionHandler.handleException(ctx, pe, PaymentResponseCode.FIELD_ERROR.getCode(), privateKey);
        } catch (Exception e) {
            exceptionHandler.handleException(ctx, e, PaymentResponseCode.UNKNOWN_ERROR.getCode(), privateKey);
        }
    }


    private void validateRequest(PaymentRequestDTO request) throws PaymentException {

        //validate token && save token in redis
        if (StringUtils.isBlank(request.getPrivateKey())) {
            throw new PaymentException("tokenKey must not be blank or contain spaces");
        }
        if (redisService.existDataFromRedis(request.getTokenKey())) {
            throw new PaymentException("tokenKey was duplicated during the day");
        }
        redisService.saveDataToRedis(request.getTokenKey(), gson.toJson(request));


        //validate phoneNumber
        if (StringUtils.isBlank(request.getMobile())) {
            throw new PaymentException("mobile - must not be blank or contain spaces");
        }
        Pattern pattern = Pattern.compile(PHONE_REGEX);

        Matcher matcher = pattern.matcher(request.getMobile());

        if (!matcher.matches()) {
            throw new PaymentException("mobile - Incorrect format of the customer's phone number.");
        }

        //validate BankCode && PrivateKey
        if (StringUtils.isBlank(request.getBankCode())) {
            throw new PaymentException("bankCode must not be blank or contain spaces");
        }
        if (StringUtils.isBlank(request.getPrivateKey())) {
            throw new PaymentException("privateKey must not be blank or contain spaces");
        }
        if (!xmlBankValidator.isValidBank(request.getBankCode(), request.getPrivateKey())) {
            throw new PaymentException("bankCode and privateKey Not the same bank.");
        }

        // So sánh hai giá trị debitAmount(số tiền thanh toán) và realAmount(số tiền sau khuyến mại)
        int comparisonResult = request.getDebitAmount().compareTo(request.getRealAmount());

        if (comparisonResult < 0) { // debitAmount nhỏ hơn realAmount
            throw new PaymentException("The payment amount must be more than the amount after the promotion.");
        } else if (comparisonResult == 0) { // debitAmount bằng realAmount
            if (!StringUtils.isEmpty(request.getPromotionCode())) {
                throw new PaymentException("Voucher code must be null or empty when there is no promotion.");
            }
        } else { // debitAmount lớn hơn realAmount
            if (StringUtils.isEmpty(request.getPromotionCode())) {
                throw new PaymentException("There is no voucher code, even though the amount has been reduced.");
            }
        }

        if (StringUtils.isEmpty(request.getPayDate())) {
            throw new PaymentException("payDate must not be blank or contain spaces");
        }
        if (!isValidPayDate(request.getPayDate())) {
            throw new PaymentException("payDate must not be blank or contain spaces");
        }

        if (StringUtils.isBlank(request.getApiId())) {
            throw new PaymentException("apiId must not be blank or contain spaces");
        }

        if (StringUtils.isBlank(request.getOderCode())) {
            throw new PaymentException("oderCode must not be blank or contain spaces");
        }

        if (StringUtils.isBlank(request.getRespCode())) {
            throw new PaymentException("respCode must not be blank or contain spaces");
        }

        if (StringUtils.isEmpty(request.getRespDesc())) {
            throw new PaymentException("respDesc must not be blank or contain spaces");
        }

        if (StringUtils.isBlank(request.getTraceTransfer())) {
            throw new PaymentException("traceTransfer must not be blank or contain spaces");
        }

        if (StringUtils.isEmpty(request.getMessageType())) {
            throw new PaymentException("messageType must not be blank or contain spaces");
        }
        if (!request.getMessageType().equals(MessageType.SUCCESS.getCode())) {
            throw new PaymentException("The value of messageType is incorrect");
        }

        if (StringUtils.isBlank(request.getAddValue().getPayMethod())) {
            throw new PaymentException("payMethod must not be blank or contain spaces");
        }
        if (request.getAddValue().getPayMethodMMS() == null) {
            throw new PaymentException("payMethodMMS must not be blank or contain spaces");
        }
    }

    private void sendDataToRabbitMQAndReply(PaymentRequestDTO message) throws JsonProcessingException {
        String messageResponse = rabbitMQService.sendMessageAndAwaitResponse(QueueName.SEND_QUEUE.getName(), message);
        ConsumerResponse consumerResponse = objectMapper.readValue(messageResponse, ConsumerResponse.class);
        if (consumerResponse.getCode().equals(PaymentResponseCode.UNKNOWN_ERROR.getCode())) {
            throw new RuntimeException("Unable to save data in database");
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
