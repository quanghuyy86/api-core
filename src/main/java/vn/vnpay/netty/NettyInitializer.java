package vn.vnpay.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import redis.clients.jedis.JedisPool;
import vn.vnpay.config.redis.RedisConfig;
import vn.vnpay.service.PaymentService;
import vn.vnpay.service.impl.PaymentServiceImpl;

public class NettyInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());

        JedisPool jedisPool = new JedisPool();
        PaymentService paymentService = new PaymentServiceImpl(jedisPool);
        pipeline.addLast(new NettyHandler(paymentService));
    }
}
