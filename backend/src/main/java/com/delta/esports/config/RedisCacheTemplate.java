package com.delta.esports.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 统一操作模板（工程化封装）：
 * <ul>
 *   <li>统一 key 前缀，避免与其它系统冲突；</li>
 *   <li>统一 JSON 序列化（Jackson）；</li>
 *   <li>统一 fail-open：任何 Redis 异常都不向上抛，get 返回 null、set/evict 静默失败，
 *       调用方照常走数据库，保证 Redis 故障不影响业务可用性。</li>
 * </ul>
 */
@Component
public class RedisCacheTemplate {

    private static final String KEY_PREFIX = "delta:";
    private static final long DEFAULT_LOCK_TTL_SECONDS = 60;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisCacheTemplate(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 读缓存；未命中 / Redis 不可用 / 反序列化失败均返回 null */
    public <T> T get(String key, TypeReference<T> type) {
        try {
            String json = redis.opsForValue().get(fullKey(key));
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = redis.opsForValue().get(fullKey(key));
            return json == null ? null : objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /** 写缓存；失败静默 */
    public void set(String key, Object value, long ttl, TimeUnit unit) {
        try {
            redis.opsForValue().set(fullKey(key), objectMapper.writeValueAsString(value), ttl, unit);
        } catch (Exception ignored) {
        }
    }

    /** 删缓存；失败静默 */
    public void evict(String key) {
        try {
            redis.delete(fullKey(key));
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * 分布式锁（SET NX EX）：抢到锁返回 true，未抢到返回 false。
     * Redis 不可用时返回 true 放行——单实例退化为普通执行，多实例场景 Redis 必然可用。
     */
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_LOCK_TTL_SECONDS);
    }

    public boolean tryLock(String key, long ttlSeconds) {
        try {
            Boolean acquired = redis.opsForValue()
                    .setIfAbsent(fullKey("lock:" + key), "1", ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired);
        } catch (RuntimeException e) {
            return true;
        }
    }

    public void unlock(String key) {
        evict("lock:" + key);
    }

    private String fullKey(String key) {
        return KEY_PREFIX + key;
    }
}
