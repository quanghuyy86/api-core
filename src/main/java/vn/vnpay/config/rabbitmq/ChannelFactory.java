package vn.vnpay.config.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.netty.channel.ChannelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import vn.vnpay.enums.RabbitMQ;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
@Slf4j
public class ChannelFactory implements PooledObjectFactory<Channel> {
    private final Connection connection;

    public ChannelFactory() {
        try {
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost(RabbitMQ.HOST.getStringValue());
            factory.setPort(RabbitMQ.PORT.getIntValue());
            factory.setUsername(RabbitMQ.USER_NAME.getStringValue());
            factory.setPassword(RabbitMQ.PASSWORD.getStringValue());

            connection = factory.newConnection();
            log.info("Connecting to RabbitMQ at {}:{} success.", RabbitMQ.HOST.getStringValue(), RabbitMQ.PORT.getIntValue());
        } catch (IOException e) {
            throw new ChannelException("Failed to create connection due to I/O error", e);
        } catch (TimeoutException e) {
            throw new ChannelException("Failed to create connection due to timeout", e);
        } catch (Exception e) {
            throw new ChannelException("Failed to create connection", e);
        }
    }

    @Override
    public PooledObject<Channel> makeObject() throws Exception {
        Channel channel = connection.createChannel();
        return new DefaultPooledObject<>(channel);
    }

    @Override
    public void destroyObject(PooledObject<Channel> pooledObject) throws Exception {
        final Channel channel = pooledObject.getObject();
        if (channel.isOpen()) {
            try {
                channel.close();
                log.info("Channel closed successfully.");
            } catch (IOException e) {
                log.error("Failed to close channel: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<Channel> pooledObject) {
        Channel channel = pooledObject.getObject();
        try {
            return channel.isOpen(); // Kiểm tra xem kênh có còn mở không
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<Channel> pooledObject) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<Channel> pooledObject) throws Exception {
    }

}
