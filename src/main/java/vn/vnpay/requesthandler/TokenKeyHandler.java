package vn.vnpay.requesthandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.service.PaymentService;

import java.util.function.BiConsumer;

@Slf4j
public class TokenKeyHandler implements BiConsumer<ChannelHandlerContext, FullHttpRequest> {
    private final PaymentService paymentService;

    public TokenKeyHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void accept(ChannelHandlerContext ctx, FullHttpRequest request) {
        log.info("Processing token key request");
        paymentService.createTokenKey(ctx, request);
    }

}
