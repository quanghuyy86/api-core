package vn.vnpay.service.impl;

import com.google.gson.Gson;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import vn.vnpay.common.enums.PaymentResponseCode;
import vn.vnpay.common.exception.PaymentException;
import vn.vnpay.common.response.BaseResponse;
import vn.vnpay.common.response.PaymentHttpResponse;
import vn.vnpay.common.response.PaymentResponse;
import vn.vnpay.config.bankcode.Bank;
import vn.vnpay.config.bankcode.XmlBankValidator;
import vn.vnpay.config.gson.GsonConfig;
import vn.vnpay.dto.payment.request.PaymentRequestDTO;
import vn.vnpay.enums.MessageType;
import vn.vnpay.service.PaymentService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final JedisPool jedisPool;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();
    private final Gson gson = GsonConfig.getGson();
    private static final String PHONE_REGEX = "^(03|05|07|08|09)[0-9]{8}$";
    private static final String currentDirectory = System.getProperty("user.dir");
    private static final String PATH_BANK_CODE = currentDirectory + "/src/main/resources/bankcode.xml";

    public PaymentServiceImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;

    }

    @Override
    public void createTokenKey(ChannelHandlerContext ctx, HttpRequest request) {
        try {
            byte[] randomBytes = new byte[24];
            secureRandom.nextBytes(randomBytes);
            String tokenKey = base64Encoder.encodeToString(randomBytes);
            log.info("tokenKey: {} ", tokenKey);
            String jsonResponse = gson.toJson(BaseResponse.success(tokenKey));
            // Gửi phản hồi về client và đóng kết nối sau khi gửi
            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(jsonResponse)).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            log.info("Error while creating tokenKey: {}", e.getMessage());
            String jsonResponse = gson.toJson(BaseResponse.unsuccessful(e.getMessage()));
            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(jsonResponse)).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void createPayment(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String requestBody = request.content().toString(CharsetUtil.UTF_8);

            PaymentRequestDTO paymentRequest = gson.fromJson(requestBody, PaymentRequestDTO.class);
            log.info("Begin create payment: {}", gson.toJson(paymentRequest));

            //validate request
            validateRequest(ctx, paymentRequest);

            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess("demo")).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            log.info("Error while creating payment: {}", e.getMessage());
            String jsonResponse = gson.toJson(BaseResponse.unsuccessful(e.getMessage()));
            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(jsonResponse)).addListener(ChannelFutureListener.CLOSE);
        }
    }

    void validateRequest(ChannelHandlerContext ctx, PaymentRequestDTO request) {
        try {
            //validate tokenKey
            validateTokenKey(request);

            //validate mobile
            validatePhoneNumber(ctx, request);

            //validate bankCode && privateKey
            validateBankCodeAndPrivateKey(request);

            //Check debitAmount, realAmount, promotionCode
            validatePromotionCode(ctx, request);

            //validate payDate
            validatePayDate(request);

            //validate apiId
            if (request.getApiId() == null || request.getApiId().isEmpty() || request.getApiId().isBlank()) {
                throw new PaymentException("apiId không được trống hoặc không có khoảng trắng");
            }

            //validate oderCode
            if (request.getOderCode() == null || request.getOderCode().isEmpty() || request.getOderCode().isBlank()) {
                throw new PaymentException("oderCode không được trống hoặc không có khoảng trắng");
            }

            //validate respCode
            if (request.getRespCode() == null || request.getRespCode().isEmpty() || request.getRespCode().isBlank()) {
                throw new PaymentException("respCode không được trống hoặc không có khoảng trắng");
            }

            //validate respDesc
            if (request.getRespDesc() == null || request.getRespDesc().isEmpty()) {
                throw new PaymentException("respDesc không được trống");
            }

            //validate traceTransfer
            if (request.getTraceTransfer() == null || request.getTraceTransfer().isEmpty()) {
                throw new PaymentException("traceTransfer không được trống");
            }

            //validate messageType
            if (request.getMessageType() == null || request.getMessageType().isEmpty()) {
                throw new PaymentException("messageType không được trống");
            }
            if (!request.getMessageType().equals(MessageType.SUCCESS.getCode())) {
                throw new PaymentException("Giá trị của messageType không chính xác");
            }

            //validate addValue
            if (request.getAddValue().getPayMethod() == null || request.getAddValue().getPayMethod().isEmpty()) {
                throw new PaymentException("payMethod không được trống");
            }
            if (request.getAddValue().getPayMethodMMS() == null) {
                throw new PaymentException("payMethodMMS không được trống");
            }

        } catch (PaymentException pe) {
            log.info(pe.getMessage());
            PaymentResponse paymentResponse = PaymentResponse.unsuccessful(
                    PaymentResponseCode.FIELD_ERROR.getCode(),
                    pe.getMessage(),
                    request.getPrivateKey()
            );
            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(gson.toJson(paymentResponse)))
                    .addListener(ChannelFutureListener.CLOSE);
        } catch (JedisConnectionException jce) {
            log.info(jce.getMessage());
            PaymentResponse paymentResponse = PaymentResponse.unsuccessful(
                    PaymentResponseCode.UNKNOWN_ERROR.getCode(),
                    jce.getMessage(),
                    request.getPrivateKey()
            );
            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(gson.toJson(paymentResponse)))
                    .addListener(ChannelFutureListener.CLOSE);
        }

    }

    private void validateTokenKey(PaymentRequestDTO request) throws PaymentException {
        if (request.getTokenKey() == null || request.getTokenKey().isEmpty() || request.getTokenKey().isBlank()) {
            throw new PaymentException("tokenKey không được để trống hoặc có khoảng trắng");
        }
        try (Jedis jedis = jedisPool.getResource()) {
            if (jedis.exists(request.getTokenKey())) {
                throw new PaymentException("tokenKey đã trùng lặp trong ngày");
            }
            long secondsUnitEndOfDay = getSecondsUntilMidnight();
            log.info("TokenKey: {} expiration time:  {} seconds", request.getTokenKey(), secondsUnitEndOfDay);
            //Save token
            jedis.setex(request.getTokenKey(), secondsUnitEndOfDay, gson.toJson(request));
        } catch (JedisConnectionException e) {
            throw new JedisConnectionException(e.getMessage());
        }
    }


    private void validatePayDate(PaymentRequestDTO request) throws PaymentException {
        if (request.getPayDate() == null || request.getPayDate().isEmpty()) {
            throw new PaymentException("payDate không được trống");
        }
        if (!isValidPayDate(request.getPayDate())) {
            throw new PaymentException("payDate không đúng định dạng");
        }
    }

    private static void validateBankCodeAndPrivateKey(PaymentRequestDTO request) throws PaymentException {
        if (request.getBankCode() == null || request.getBankCode().isEmpty()) {
            throw new PaymentException("bankCode không được trống.");
        }
        if (request.getPrivateKey() == null || request.getPrivateKey().isEmpty()) {
            throw new PaymentException("privateKey không được trống.");
        }
        XmlBankValidator validator = new XmlBankValidator();
        List<Bank> banks = validator.readBankFromXml(PATH_BANK_CODE);
        if (!validator.isValidBank(request.getBankCode(), request.getPrivateKey(), banks)) {
            throw new PaymentException("bankCode and privateKey không cùng 1 ngân hàng.");
        }
    }

    private void validatePromotionCode(ChannelHandlerContext ctx, PaymentRequestDTO request) throws PaymentException {
        // So sánh hai giá trị debitAmount(số tiền thanh toán) và realAmount(số tiền sau khuyến mại)
        int comparisonResult = request.getDebitAmount().compareTo(request.getRealAmount());

        if (comparisonResult < 0) { // debitAmount nhỏ hơn realAmount
            throw new PaymentException("Số tiền thanh toán phải hơn số tiền sau khuyến mại.");
        } else if (comparisonResult == 0) { // debitAmount bằng realAmount
            if (request.getPromotionCode() != null && !request.getPromotionCode().isEmpty()) {
                throw new PaymentException("Mã Voucher phải bằng null hoặc rỗng khi không có khuyến mãi.");
            }
        } else { // debitAmount lớn hơn realAmount
            if (request.getPromotionCode() == null || request.getPromotionCode().isEmpty()) {
                throw new PaymentException("Error code 01: Không có mã voucher, mặc dù số tiền đã giảm.");
            }
        }
    }

    private void validatePhoneNumber(ChannelHandlerContext ctx, PaymentRequestDTO request) throws PaymentException {
        if (request.getMobile() == null || request.getMobile().isEmpty()) {
            throw new PaymentException("mobile - Số điện thoại của khách hàng không đuợc để trống.");
        }
        Pattern pattern = Pattern.compile(PHONE_REGEX);

        Matcher matcher = pattern.matcher(request.getMobile());

        if (!matcher.matches()) {
            throw new PaymentException("mobile - Sai định dạng số điện thoại của khách hàng.");
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

    // Hàm tính số giây còn lại từ hiện tại đến 0h ngày hôm sau
    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return ChronoUnit.SECONDS.between(now, midnight);
    }
}
