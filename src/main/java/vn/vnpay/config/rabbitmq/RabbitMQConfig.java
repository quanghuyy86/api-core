package vn.vnpay.config.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import vn.vnpay.enums.ExchangeName;
import vn.vnpay.enums.QueueName;

public class RabbitMQConfig {
    private final ChannelPool channelPool;
    private final Channel channel;

    public RabbitMQConfig() throws Exception {
        this.channelPool = ChannelPool.getInstance();

        // Lấy một Channel từ pool
        channel = channelPool.borrowChannel();


        //Khai báo exchange và queue
        channel.exchangeDeclare(ExchangeName.MY_EXCHANGE.getName(), BuiltinExchangeType.DIRECT);
        channel.queueDeclare(QueueName.MY_QUEUE.getName(), false, false, false, null);
        channel.queueBind(QueueName.MY_QUEUE.getName(), ExchangeName.MY_EXCHANGE.getName(), "nettyRoutingKey");

        // Trả lại Channel vào pool
        channelPool.returnChannel(channel);
    }

    // Phương thức để lấy Channel từ pool khi cần
    public Channel getChannelFromPool() throws InterruptedException {
        return channelPool.borrowChannel();
    }

    // Phương thức để trả lại Channel vào pool sau khi sử dụng
    public void returnChannelToPool(Channel channel) {
        channelPool.returnChannel(channel);
    }

}
