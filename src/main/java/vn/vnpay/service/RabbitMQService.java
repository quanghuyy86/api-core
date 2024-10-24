package vn.vnpay.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;
import io.netty.channel.ChannelException;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.config.rabbitmq.ChannelPool;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class RabbitMQService {
    private final ChannelPool channelPool;

    public RabbitMQService(ChannelPool channelPool) {
        this.channelPool = channelPool;
    }

    public void sendMessage(String queueName, String message) {
        Channel channel = null;
        try {
            channel = channelPool.getChannel(); // Lấy kênh từ pool
            channel.queueDeclare(queueName, true, false, false, null); // Đảm bảo hàng đợi tồn tại
            channel.basicPublish("", queueName, null, message.getBytes("UTF-8")); // Gửi thông điệp
            System.out.println(" [x] Sent '" + message + "'");
        } catch (Exception e) {
            throw new ChannelException("Could not send message", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }

    public void receiveMessages(String queueName) {
        Channel channel = null;
        try {
            channel = channelPool.getChannel(); // Lấy kênh từ pool
            channel.queueDeclare(queueName, true, false, false, null); // Đảm bảo hàng đợi tồn tại

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
                //Xử lý message
            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            throw new ChannelException("Could not receive messages", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }

    public String sendMessageAndWaitForResponse(String queueName, String message) {
        Channel channel = null;
        final String correlationId = UUID.randomUUID().toString();
        BlockingQueue<String> response = new ArrayBlockingQueue<>(1); // Hàng đợi để chờ phản hồi

        try {
            channel = channelPool.getChannel();
            channel.queueDeclare(queueName, true, false, false, null); // Đảm bảo hàng đợi tồn tại

            // Cài đặt DeliverCallback để nhận phản hồi
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    String responseMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    response.offer(responseMessage); // Đưa phản hồi vào hàng đợi
                }
            };

            // Đăng ký consumer cho phản hồi
            String replyQueueName = channel.queueDeclare().getQueue(); // Tạo hàng đợi cho phản hồi
            channel.basicConsume(replyQueueName, true, deliverCallback, consumerTag -> {});

            // Gửi thông điệp với thuộc tính trả lời
            AMQP.BasicProperties props = MessageProperties.PERSISTENT_TEXT_PLAIN.builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();
            channel.basicPublish("", queueName, props, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "' and waiting for response...");

            // Chờ phản hồi
            return response.take();
        } catch (Exception e) {
            throw new ChannelException("Could not send message and wait for response: " +  e.getMessage());
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel);
            }
        }
    }

}
