package vn.vnpay.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public class NettyServer {
    static final Map<String, Map<HttpMethod, BiConsumer<ChannelHandlerContext, FullHttpRequest>>> ROUTE_MAP = new HashMap<>();
    private final int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public NettyServer addRoute(String path, HttpMethod method, BiConsumer<ChannelHandlerContext, FullHttpRequest> handler) {
        ROUTE_MAP.computeIfAbsent(path, k -> new HashMap<>()).put(method, handler);
        return this;
    }

    public void start() throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyInitializer());
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            log.info("Server started on port {} ", port);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static Map<String, Map<HttpMethod, BiConsumer<ChannelHandlerContext, FullHttpRequest>>> getRouteMap() {
        return ROUTE_MAP;
    }
}
