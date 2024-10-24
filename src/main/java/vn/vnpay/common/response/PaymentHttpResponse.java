package vn.vnpay.common.response;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class PaymentHttpResponse {
    public static FullHttpResponse responseSuccess(String jsonResponse){
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(jsonResponse, CharsetUtil.UTF_8));
        response.headers().set("Content-Type", "application/json");
        response.headers().set("Content-Length", response.content().readableBytes());
        return response;
    }

    public static FullHttpResponse errorResponseSuccess(String jsonResponse){
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(jsonResponse, CharsetUtil.UTF_8));
        response.headers().set("Content-Type", "application/json");
        response.headers().set("Content-Length", response.content().readableBytes());
        return response;
    }

    public static FullHttpResponse errorResponseFail(String jsonResponse, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(jsonResponse, CharsetUtil.UTF_8));
        response.headers().set("Content-Type", "application/json");
        response.headers().set("Content-Length", response.content().readableBytes());
        return response;
    }


}
