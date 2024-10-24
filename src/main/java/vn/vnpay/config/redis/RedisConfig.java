package vn.vnpay.config.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
@Slf4j
public class RedisConfig {
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_HOST = "localhost";
    private static final int MAX_TOTAL_CONNECTIONS = 128;  // Số kết nối tối đa
    private static final int MAX_IDLE_CONNECTIONS = 64; // Số kết nối tối đa có thể tồn tại trong trạng thái nhàn rỗi
    private static final int MIN_IDLE_CONNECTIONS = 16; // Số kết nối tối thiểu luôn được duy trì
    private final JedisPool jedisPool;
    public RedisConfig() {
        log.info("Initializing Redis configuration...");
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        poolConfig.setMaxIdle(MAX_IDLE_CONNECTIONS);
        poolConfig.setMinIdle(MIN_IDLE_CONNECTIONS);

        // Khởi tạo JedisPool
        jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
        log.info("JedisPool initialized successfully with maxTotal={}, maxIdle={}, minIdle={}.", MAX_TOTAL_CONNECTIONS, MAX_IDLE_CONNECTIONS, MIN_IDLE_CONNECTIONS);
    }

    public JedisPool getJedisPool() {
        log.info("Getting JedisPool...");
        return jedisPool;
    }

    public void close() {
        log.info("Closing JedisPool...");
        if (jedisPool != null) {
            jedisPool.close();
            log.info("JedisPool closed successfully.");
        } else {
            log.info("JedisPool is already null, nothing to close.");
        }
    }
}
