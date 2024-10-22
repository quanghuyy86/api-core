package vn.vnpay;

import io.netty.handler.codec.http.HttpMethod;
import redis.clients.jedis.JedisPool;
import vn.vnpay.common.exception.GlobalExceptionHandler;
import vn.vnpay.config.rabbitmq.RabbitMQConfig;
import vn.vnpay.config.redis.RedisConfig;
import vn.vnpay.enums.Route;
import vn.vnpay.netty.NettyServer;
import vn.vnpay.requesthandler.PaymentHandler;
import vn.vnpay.requesthandler.TokenKeyHandler;
import vn.vnpay.service.PaymentService;
import vn.vnpay.service.RedisService;
import vn.vnpay.service.impl.PaymentServiceImpl;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        RedisConfig redisConfig = new RedisConfig();
        JedisPool jedisPool = redisConfig.getJedisPool();
        RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        RedisService redisService = new RedisService(jedisPool);
        PaymentService paymentService = new PaymentServiceImpl(rabbitMQConfig, redisService, exceptionHandler);

        new NettyServer(port)
                .addRoute(Route.CREATE_PAYMENT.getPath(), HttpMethod.POST, new PaymentHandler(paymentService))
                .addRoute(Route.TOKEN_KEY.getPath(),HttpMethod.GET, new TokenKeyHandler(paymentService))
                .start();
    }
}

