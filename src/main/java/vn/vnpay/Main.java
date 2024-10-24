package vn.vnpay;

import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpMethod;
import redis.clients.jedis.JedisPool;
import vn.vnpay.common.exception.GlobalExceptionHandler;
import vn.vnpay.config.bankcode.XmlBankValidator;
import vn.vnpay.config.gson.GsonConfig;
import vn.vnpay.config.rabbitmq.ChannelPool;
import vn.vnpay.config.redis.RedisConfig;
import vn.vnpay.enums.Route;
import vn.vnpay.netty.NettyServer;
import vn.vnpay.requesthandler.PaymentHandler;
import vn.vnpay.requesthandler.TokenKeyHandler;
import vn.vnpay.service.PaymentService;
import vn.vnpay.service.RabbitMQService;
import vn.vnpay.service.RedisService;
import vn.vnpay.service.impl.PaymentServiceImpl;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        RedisConfig redisConfig = new RedisConfig();
        JedisPool jedisPool = redisConfig.getJedisPool();
        RedisService redisService = new RedisService(jedisPool);

        ChannelPool channelPool = new ChannelPool();
        RabbitMQService rabbitMQService = new RabbitMQService(channelPool);

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

        XmlBankValidator xmlBankValidator = new XmlBankValidator();

        Gson gson = GsonConfig.getGson();

        PaymentService paymentService = new PaymentServiceImpl(redisService, exceptionHandler, rabbitMQService, xmlBankValidator, gson);

        new NettyServer(port)
                .addRoute(Route.CREATE_PAYMENT.getPath(), HttpMethod.POST, new PaymentHandler(paymentService))
                .addRoute(Route.TOKEN_KEY.getPath(),HttpMethod.GET, new TokenKeyHandler(paymentService))
                .start();
    }
}

