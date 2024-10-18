package vn.vnpay.common.util;

import com.google.gson.Gson;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import vn.vnpay.common.response.PaymentHttpResponse;
import vn.vnpay.common.response.PaymentResponse;
import vn.vnpay.config.gson.GsonConfig;

public class ResponseUtil {
    private static final Gson GSON = GsonConfig.getGson();
    public static void sendErrorResponse(ChannelHandlerContext ctx, String errorCode, String errorMessage, String errorFieldValue) {
        PaymentResponse paymentResponse = PaymentResponse.unsuccessful(
                errorCode,
                errorMessage,
                errorFieldValue
        );

        ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(GSON.toJson(paymentResponse)))
                .addListener(ChannelFutureListener.CLOSE);
    }
}
