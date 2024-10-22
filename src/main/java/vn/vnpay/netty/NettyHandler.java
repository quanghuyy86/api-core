package vn.vnpay.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.common.exception.GlobalExceptionHandler;

import java.util.Map;
import java.util.function.BiConsumer;

import static vn.vnpay.netty.NettyServer.ROUTE_MAP;


@Slf4j
public class NettyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        HttpMethod method = request.method();
        log.info("Received request: {} {}", method, uri);

        // Lấy Map phương thức ánh xạ với handler từ URI
        Map<HttpMethod, BiConsumer<ChannelHandlerContext, FullHttpRequest>> methodHandlerMap = ROUTE_MAP.get(uri);

        if (methodHandlerMap != null) {
            // Lấy handler cho phương thức tương ứng
            BiConsumer<ChannelHandlerContext, FullHttpRequest> handler = methodHandlerMap.get(method);

            if (handler != null) {
                handler.accept(ctx, request); // Thực thi handler tương ứng với phương thức
            } else {
                new GlobalExceptionHandler().handleInvalidMethod(ctx); // Phương thức không hợp lệ (405)
            }
        } else {
            new GlobalExceptionHandler().handleBadRequest(ctx); // URI không hợp lệ (404)
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught: ", cause);
        ctx.close();
    }
}
