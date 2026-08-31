package com.delta.esports.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
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
    /** Redis 故障时的 fail-open 标记；释放时不执行任何 Redis 删除。 */
    private static final String LOCAL_FALLBACK_TOKEN = "";
    private static final DefaultRedisScript<Long> SAFE_UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

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
     * 分布式锁（SET NX EX）：成功返回本次锁的唯一令牌，未抢到返回 null。
     * Redis 不可用时返回空令牌放行，调用方仍可依赖数据库幂等；空令牌释放时为 no-op。
     */
    public String acquireLock(String key) {
        return acquireLock(key, DEFAULT_LOCK_TTL_SECONDS);
    }

    public String acquireLock(String key, long ttlSeconds) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue()
                    .setIfAbsent(fullKey("lock:" + key), token, ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired) ? token : null;
        } catch (RuntimeException e) {
            return LOCAL_FALLBACK_TOKEN;
        }
    }

    /** 仅当令牌仍属于当前调用者时删除锁，避免误删过期后由其他实例重新取得的锁。 */
    public void releaseLock(String key, String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        try {
            redis.execute(SAFE_UNLOCK_SCRIPT,
                    Collections.singletonList(fullKey("lock:" + key)), token);
        } catch (RuntimeException ignored) {
            // TTL 会最终释放锁；Redis 故障不应阻断业务收尾。
        }
    }

    private String fullKey(String key) {
        return KEY_PREFIX + key;
    }
}
