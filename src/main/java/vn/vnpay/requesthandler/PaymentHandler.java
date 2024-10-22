package vn.vnpay.requesthandler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.common.exception.GlobalExceptionHandler;
import vn.vnpay.common.response.PaymentHttpResponse;
import vn.vnpay.enums.Route;
import vn.vnpay.service.PaymentService;

import java.util.function.BiConsumer;


@Slf4j
public class PaymentHandler implements BiConsumer<ChannelHandlerContext, FullHttpRequest> {
    private final PaymentService paymentService;

    public PaymentHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void accept(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) {
        log.info("Processing payment request");
        paymentService.createPayment(channelHandlerContext, fullHttpRequest);

    }
}
