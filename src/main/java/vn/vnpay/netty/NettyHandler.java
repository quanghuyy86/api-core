package vn.vnpay.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.common.enums.PaymentResponseCode;
import vn.vnpay.common.response.PaymentHttpResponse;
import vn.vnpay.dto.payment.request.PaymentRequestDTO;
import vn.vnpay.enums.Route;
import vn.vnpay.service.PaymentService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public class NettyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final PaymentService paymentService;
    private final Map<String, BiConsumer<ChannelHandlerContext, FullHttpRequest>> routemap = new HashMap<>();

    public NettyHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
        routemap.put(Route.TOKEN_KEY.getPath(), this::handlerCreateTokenKey);
        routemap.put(Route.CREATE_PAYMENT.getPath(), this::handlerCreatePayment);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        HttpMethod method = request.method();
        log.info("Received request: {} {}", request.method(), uri);

        BiConsumer<ChannelHandlerContext, FullHttpRequest> handler = routemap.get(uri);

        if (method.equals(HttpMethod.POST)) {
            if (handler != null) {
                handler.accept(ctx, request);
            } else {
                ctx.writeAndFlush(PaymentHttpResponse.responseError(PaymentResponseCode.UNKNOWN_ERROR.getMessage(), HttpResponseStatus.NOT_FOUND)).addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            ctx.writeAndFlush(PaymentHttpResponse.responseError("Method Not Allowed", HttpResponseStatus.METHOD_NOT_ALLOWED))
                    .addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handlerCreateTokenKey(ChannelHandlerContext ctx, FullHttpRequest request) {
        paymentService.createTokenKey(ctx, request);
    }

    private void handlerCreatePayment(ChannelHandlerContext ctx, FullHttpRequest request) {
        paymentService.createPayment(ctx, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught: ", cause);
        ctx.close();
    }
}
