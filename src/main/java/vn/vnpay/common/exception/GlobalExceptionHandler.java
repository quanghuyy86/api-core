package vn.vnpay.common.exception;

import com.google.gson.Gson;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.common.enums.PaymentResponseCode;
import vn.vnpay.common.response.PaymentHttpResponse;
import vn.vnpay.common.response.PaymentResponse;
import vn.vnpay.config.gson.GsonConfig;

import java.sql.SQLException;

@Slf4j
public class GlobalExceptionHandler {
    private final Gson gson = GsonConfig.getGson();

    public void handleException(ChannelHandlerContext ctx, Exception exception, String code, String privateKey) {
        if (exception instanceof PaymentException) {
            handlePaymentException(ctx, code, (PaymentException) exception, privateKey);
        } else if (exception instanceof SQLException) {
            handleSQLException(ctx, (SQLException) exception, privateKey);
        } else {
            handleGenericException(ctx, exception, privateKey);
        }
    }

    private void handlePaymentException(ChannelHandlerContext ctx, String code,
                                        PaymentException pe, String privateKey) {
        log.info(pe.getMessage());
        PaymentResponse paymentResponse = PaymentResponse.unsuccessful(
                code,
                pe.getMessage(),
                privateKey
        );
        ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(gson.toJson(paymentResponse)))
                .addListener(ChannelFutureListener.CLOSE);
    }

    private void handleSQLException(ChannelHandlerContext ctx, SQLException exception, String privateKey) {
        log.error("SQL Error: {}", exception.getMessage());
        PaymentResponse paymentResponse = PaymentResponse.unsuccessful(
                PaymentResponseCode.UNKNOWN_ERROR.getCode(),
                exception.getMessage(),
                privateKey
        );
        ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(gson.toJson(paymentResponse)))
                .addListener(ChannelFutureListener.CLOSE);
    }

    private void handleGenericException(ChannelHandlerContext ctx, Exception exception, String privateKey) {
        log.error("Unexpected Error: {}", exception.getMessage());
        PaymentResponse paymentResponse = PaymentResponse.unsuccessful(
                PaymentResponseCode.UNKNOWN_ERROR.getCode(),
                exception.getMessage(),
                privateKey
        );
        ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(gson.toJson(paymentResponse)))
                .addListener(ChannelFutureListener.CLOSE);
    }

    //Method không hợp lệ
    public void handleInvalidMethod(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(PaymentHttpResponse
                        .errorResponseFail(HttpResponseStatus.METHOD_NOT_ALLOWED.reasonPhrase(), HttpResponseStatus.METHOD_NOT_ALLOWED))
                .addListener(ChannelFutureListener.CLOSE);
    }

    //Request không hợp lệ
    public void handleBadRequest(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(PaymentHttpResponse
                        .errorResponseFail(HttpResponseStatus.BAD_REQUEST.reasonPhrase(), HttpResponseStatus.NOT_FOUND))
                .addListener(ChannelFutureListener.CLOSE);
    }

}
