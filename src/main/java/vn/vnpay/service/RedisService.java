package vn.vnpay.service;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
@Slf4j
public class RedisService {
    private final JedisPool jedisPool;

    public RedisService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void saveDataToRedis(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
            long secondsUntilMidnight = getSecondsUntilMidnight();
            jedis.expire(key, secondsUntilMidnight);
            log.info("Saved to Redis with key: {}, expiration time: {} second", key, secondsUntilMidnight);
        } catch (JedisConnectionException jce) {
            throw new JedisConnectionException("Error when interacting with Redis: " + jce.getMessage());
        }
    }

    public String getDataFromRedis(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (JedisConnectionException jce) {
            throw new JedisConnectionException("Lỗi khi tương tác với Redis: " + jce.getMessage());
        }
    }

    public boolean existDataFromRedis(String key){
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (JedisConnectionException jce) {
            throw new JedisConnectionException("Lỗi khi tương tác với Redis: " + jce.getMessage());
        }
    }


    // Ví dụ hàm tính số giây đến hết ngày
    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return ChronoUnit.SECONDS.between(now, midnight);
    }
}
