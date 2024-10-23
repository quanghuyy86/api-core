package vn.vnpay.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.config.rabbitmq.RabbitMQConfig;
import vn.vnpay.enums.ExchangeName;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class RabbitMQService {
    private final RabbitMQConfig rabbitMQConfig;

    public RabbitMQService(RabbitMQConfig rabbitMQConfig) {
        this.rabbitMQConfig = rabbitMQConfig;
    }

    // Phương thức để gửi message đến RabbitMQ (không yêu cầu phản hồi)
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

            log.info("Message sent to exchange: {}, routingKey: {}, message: {}",
                    ExchangeName.MY_EXCHANGE.getName(), routingKey, message);

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

    // Phương thức để gửi message đến RabbitMQ và nhận phản hồi (RPC)
    public String sendMessageWithReply(String message, String routingKey) throws Exception {
        Channel channel = null;
        String response = null;
        try {
            // Lấy Channel từ pool
            channel = rabbitMQConfig.getChannelFromPool();

            // Tạo một queue tạm thời cho RPC
            String replyQueue = channel.queueDeclare().getQueue();
            final String corrId = java.util.UUID.randomUUID().toString();

            // Cài đặt thuộc tính message để đính kèm corrId và replyQueue
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueue)
                    .build();

            // Gửi message tới exchange với routingKey và thuộc tính props
            channel.basicPublish(
                    ExchangeName.MY_EXCHANGE.getName(),
                    routingKey,
                    props,
                    message.getBytes("UTF-8")
            );

            log.info("Message sent to exchange: {}, routingKey: {}, message: {}, correlationId: {}",
                    ExchangeName.MY_EXCHANGE.getName(), routingKey, message, corrId);

            // Tạo một consumer để lắng nghe phản hồi trên replyQueue
            final BlockingQueue<String> responseQueue = new ArrayBlockingQueue<>(1);
            String consumerTag = channel.basicConsume(replyQueue, true, (consumerTag1, delivery) -> {
                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    responseQueue.offer(new String(delivery.getBody(), "UTF-8"));
                }
            }, consumerTag1 -> {});

            //Đang xử lý response
            // Chờ phản hồi từ RabbitMQ
            response = responseQueue.take();

            log.info("Received response: {}", response);

        } catch (Exception e) {
            log.error("Failed to send message with reply: ", e);
            throw e;
        } finally {
            // Trả Channel về pool
            if (channel != null) {
                rabbitMQConfig.returnChannelToPool(channel);
            }
        }
        return response;
    }

}
