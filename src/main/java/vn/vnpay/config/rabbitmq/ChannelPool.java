package vn.vnpay.config.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.enums.RabbitMQ;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
@Slf4j
public class ChannelPool {
    private static ChannelPool instance;
    private final Connection connection;
    private final BlockingQueue<Channel> pool;

    public ChannelPool() throws Exception {
        //cấu hình kết nối
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(RabbitMQ.HOST.getStringValue());
        connectionFactory.setPort(RabbitMQ.PORT.getIntValue());
        connectionFactory.setUsername(RabbitMQ.USER_NAME.getStringValue());
        connectionFactory.setPassword(RabbitMQ.PASSWORD.getStringValue());

        this.connection = connectionFactory.newConnection();
        // Log địa chỉ IP của kết nối
        log.info("Connection established to RabbitMQ at: {}", connection.getAddress());

        //tạo pool với kích thước = poolSize
        this.pool = new LinkedBlockingDeque<>(RabbitMQ.POOL_SIZE.getIntValue());

        // Khởi tạo các Channel và lưu vào pool
        for (int i = 0; i < RabbitMQ.POOL_SIZE.getIntValue(); i++) {
            Channel channel = connection.createChannel();
            pool.offer(channel); // Thêm Channel vào pool
        }
    }

    public static synchronized ChannelPool getInstance() throws Exception {
        if (instance == null) {
            instance = new ChannelPool();
        }
        return instance;
    }

    // Lấy Channel từ pool
    public Channel borrowChannel() throws InterruptedException {
        try {
            return pool.take(); // Lấy một Channel ra từ pool
        } catch (IllegalAccessError e) {
            throw new RuntimeException("Error borrowing channel from pool: " + e.getMessage());
        }
    }

    // Trả lại Channel vào pool
    public void returnChannel(Channel channel) {
        if (channel != null) {
            try {
                pool.offer(channel); // Trả lại Channel vào pool để tái sử dụng
            } catch (Exception e) {
                throw new RuntimeException("Error returning channel to pool: " + e.getMessage());
            }
        }
    }

    // Đóng toàn bộ các Channel và kết nối
    public void close() throws Exception {
        for (Channel channel : pool) {
            channel.close(); // Đóng từng Channel
        }
        connection.close(); // Đóng kết nối đến RabbitMQ
    }
}
