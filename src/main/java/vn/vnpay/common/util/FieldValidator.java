package vn.vnpay.common.util;

import com.google.gson.Gson;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.common.enums.PaymentResponseCode;
import vn.vnpay.common.response.PaymentHttpResponse;
import vn.vnpay.common.response.PaymentResponse;
import vn.vnpay.dto.payment.request.PaymentRequestDTO;

@Slf4j
public class FieldValidator {
    // Hàm kiểm tra field và gửi phản hồi nếu null hoặc empty
    public static void validateFieldAndRespond(String fieldName, String filedValue, PaymentRequestDTO request,
                                               ChannelHandlerContext ctx,
                                               Gson gson){
        if (filedValue == null || filedValue.isEmpty()){
            PaymentResponse paymentResponse = PaymentResponse.unsuccessful(
                    PaymentResponseCode.FIELD_ERROR.getCode(),
                    fieldName + " null or empty",
                    request.getPrivateKey()
            );
            log.info("{}: null or empty", fieldName);
            ctx.writeAndFlush(PaymentHttpResponse.errorResponseSuccess(gson.toJson(paymentResponse)))
                    .addListener(ChannelFutureListener.CLOSE);
        }

    }



}
