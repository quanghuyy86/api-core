package vn.vnpay.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import vn.vnpay.dto.payment.request.PaymentRequestDTO;

public interface PaymentService {
    void createTokenKey(ChannelHandlerContext ctx, HttpRequest request);

    void createPayment(ChannelHandlerContext ctx, FullHttpRequest request);
}
