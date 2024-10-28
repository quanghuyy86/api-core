package vn.vnpay.service;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import io.netty.channel.ChannelException;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.config.rabbitmq.ChannelPool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

@Slf4j
public class RabbitMQService {
    private final Gson gson;
    private final ChannelPool channelPool;

    public RabbitMQService(Gson gson, ChannelPool channelPool) {
        this.gson = gson;
        this.channelPool = channelPool;
    }

    //Gửi message lên exchange
    public void sendMessage(String exchangeName, String routingKeyName, Object message) {
        Channel channel = null;
        try {
            channel = channelPool.getChannel(); // Lấy kênh từ pool

            // Khai báo exchange (không gây lỗi nếu exchange đã tồn tại)
            channel.exchangeDeclare(exchangeName, "direct", true);
            String sendMessage = gson.toJson(message);

            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) //độ bền của message (1 = không bền, 2 = bền)
                    .build();

            channel.basicPublish(exchangeName, routingKeyName, properties, sendMessage.getBytes(StandardCharsets.UTF_8));
            log.info("Đã gửi message: {} tới Exchange: {} và có RoutingKey: {}", sendMessage, exchangeName, routingKeyName);
        } catch (Exception e) {
            throw new ChannelException("Could not send message", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }

    //Gửi message lên queue
    public void sendMessage(String queueName, Object message) {
        Channel channel = null;
        try {
            channel = channelPool.getChannel(); // Lấy kênh từ pool

            // Khai báo queue (không gây lỗi nếu queue đã tồn tại)
            channel.queueDeclare(queueName, true, false, false, null);
            String sendMessage = gson.toJson(message);

            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) //độ bền của message (1 = không bền, 2 = bền)
                    .build();

            channel.basicPublish("", queueName, properties, sendMessage.getBytes(StandardCharsets.UTF_8));
            log.info("Đã gửi message: {} trực tiếp tới queue name: {}.", sendMessage, queueName);
        } catch (Exception e) {
            throw new ChannelException("Could not send message", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }

    //Nhận message
    public void receiveMessages(String exchangeName, String routingKeyName, String queueName) {
        Channel channel = null;
        try {
            channel = channelPool.getChannel();

            channel.exchangeDeclare(exchangeName, "direct", true);
            channel.queueDeclare(queueName, true, false, false, null);

            channel.queueBind(queueName, exchangeName, routingKeyName);

            Channel finalChannel = channel;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);

                // Xác nhận message đã được xử lý
                finalChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            // Bắt đầu lắng nghe message từ queue
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
                // Xử lý khi hủy đăng ký
                System.out.println("Consumer cancelled: " + consumerTag);
            });

        } catch (IOException ie) {
            throw new ChannelException("Failed to bind queue to exchange:  " + ie.getMessage());
        } catch (Exception e) {
            throw new ChannelException("Could not receive message", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }

    public String receiveMessages(String queueName) {
        Channel channel = null;
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        try {
            channel = channelPool.getChannel();
            // Khai báo queue để đảm bảo queue tồn tại
            channel.queueDeclare(queueName, true, false, false, null);

            // Thiết lập callback để xử lý message khi nhận đc
            Channel finalChannel = channel;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);

                messageFuture.complete(message);

                // Xác nhận message đã được xử lý
                finalChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            // Bắt đầu lắng nghe message từ queue
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
                System.out.println("Consumer cancelled: " + consumerTag);
            });

            return messageFuture.get();

        } catch (IOException e) {
            throw new ChannelException("Could not receive message from queue", e);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }

    public String sendMessageAndAwaitResponse(String queueName, Object message) {
        Channel channel = null;
        String responseQueueName = "responseQueue_" + UUID.randomUUID(); // Tạo queue phản hồi duy nhất
        CompletableFuture<String> responseFuture = new CompletableFuture<>();

        try {
            channel = channelPool.getChannel();

            // Khai báo queue (không gây lỗi nếu queue đã tồn tại)
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueDeclare(responseQueueName, true, false, false, null);

            String sendMessage = gson.toJson(message);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // độ bền của message (1 = không bền, 2 = bền)
                    .replyTo(responseQueueName) // Đặt queue để nhận phản hồi
                    .correlationId(UUID.randomUUID().toString()) // Thêm ID duy nhất cho mỗi thông điệp
                    .build();

            channel.basicPublish("", queueName, properties, sendMessage.getBytes(StandardCharsets.UTF_8));
            log.info("Message sent to queue {}: {} ", queueName, sendMessage);

            // Tạo consumer cho phản hồi
            channel.basicConsume(responseQueueName, true, (consumerTag, delivery) -> {
                String response = new String(delivery.getBody(), StandardCharsets.UTF_8);
                log.info("Response received: {}", response);
                responseFuture.complete(response);
            }, consumerTag -> {
                System.out.println("Consumer cancelled: " + consumerTag);
            });

            // Chờ phản hồi
            return responseFuture.get(); // Chờ cho đến khi nhận được phản hồi

        } catch (IOException e) {
            throw new ChannelException("Could not send message", e);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }


    public void receiveAndRespond(String queueName) {
        Channel channel = null;
        try {
            channel = channelPool.getChannel();
            channel.queueDeclare(queueName, true, false, false, null);

            // Thiết lập callback để xử lý message khi nhận được
            Channel finalChannel = channel;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);

                // Xử lý logic của message ở đây
                boolean success = processMessage(message); // Phương thức để xử lý message và trả về true/false

                // Gửi phản hồi trở lại
                String responseMessage = success ? "Success" : "Failure";
                String responseQueue = delivery.getProperties().getReplyTo(); // Lấy queue để phản hồi

                finalChannel.basicPublish("", responseQueue, null, responseMessage.getBytes(StandardCharsets.UTF_8));
                System.out.println("Sent response: " + responseMessage);

                // Xác nhận message đã được xử lý
                finalChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            // Bắt đầu lắng nghe message từ queue
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
                System.out.println("Consumer cancelled: " + consumerTag);
            });

        } catch (IOException e) {
            throw new ChannelException("Could not receive message from queue", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel); // Trả kênh lại cho pool
            }
        }
    }

    private boolean processMessage(String message) {
        return true;
    }



}






