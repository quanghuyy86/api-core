package vn.vnpay.config.rabbitmq;

import com.rabbitmq.client.Channel;
import io.netty.channel.ChannelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.NoSuchElementException;

@Slf4j
public class ChannelPool implements Cloneable {
    private GenericObjectPool<Channel> internalPool;
    public static GenericObjectPoolConfig<Channel> defaultConfig;

    static {
        defaultConfig = new GenericObjectPoolConfig<>();
        defaultConfig.setMaxTotal(10);
        defaultConfig.setMaxIdle(10);
        defaultConfig.setMinIdle(5);
        defaultConfig.setBlockWhenExhausted(true); // Chặn khi không còn kênh
        log.info("Pool Config: Max Total = {}, Max Idle = {}, Min Idle = {}, Block When Exhausted = {}",
                defaultConfig.getMaxTotal(), defaultConfig.getMaxIdle(), defaultConfig.getMinIdle(),
                defaultConfig.getBlockWhenExhausted());
    }

    public ChannelPool() {
        this(defaultConfig, new ChannelFactory());
    }

    public ChannelPool(final GenericObjectPoolConfig<Channel> poolConfig, ChannelFactory factory) {
        if (this.internalPool != null) {
            try {
                closeInternalPool();
            } catch (Exception e) {
                log.info("Error while closing internal pool: {}", e.getMessage());
            }
        }

        this.internalPool = new GenericObjectPool<Channel>(factory, poolConfig);
    }

    private void closeInternalPool() {
        try {
            internalPool.close();
        } catch (Exception e) {
            throw new ChannelException("Could not destroy the pool", e);
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                internalPool.returnObject(channel);
            } else {
                internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            throw new ChannelException("Could not return the resource to the pool", e);
        }
    }

    public Channel getChannel() {
        try {
            return internalPool.borrowObject();
        } catch (NoSuchElementException nse) {
            if (null == nse.getCause()) { // The exception was caused by an exhausted pool
                throw new ChannelException("Could not get a resource since the pool is exhausted", nse);
            }
            // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
            throw new ChannelException("Could not get a resource from the pool", nse);
        } catch (Exception e) {
            throw new ChannelException("Could not get a resource from the pool", e);
        }
    }

    @Override
    public ChannelPool clone() {
        try {
            return (ChannelPool) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
