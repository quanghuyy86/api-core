package vn.vnpay.service;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.config.rabbitmq.RabbitMQConfig;
import vn.vnpay.enums.ExchangeName;

@Slf4j
public class RabbitMQService {
    private final RabbitMQConfig rabbitMQConfig;

    public RabbitMQService(RabbitMQConfig rabbitMQConfig) {
        this.rabbitMQConfig = rabbitMQConfig;
    }

    // Phương thức để gửi message đến RabbitMQ
    public void sendMessage(String message, String routingKey) throws Exception {
        Channel channel = null;
        try {
            // Lấy Channel từ pool
            channel = rabbitMQConfig.getChannelFromPool();

            // Gửi message tới exchange với routingKey
            channel.basicPublish(
                    ExchangeName.MY_EXCHANGE.getName(),
                    routingKey,
                    null,
                    message.getBytes("UTF-8")
            );

            log.info("Message sent to exchange: {}, routingKey: {}, message: {}", ExchangeName.MY_EXCHANGE.getName(), routingKey, message);

        } catch (Exception e) {
            log.error("Failed to send message: ", e);
            throw e;
        } finally {
            // Trả Channel về pool
            if (channel != null) {
                rabbitMQConfig.returnChannelToPool(channel);
            }
        }
    }
}
