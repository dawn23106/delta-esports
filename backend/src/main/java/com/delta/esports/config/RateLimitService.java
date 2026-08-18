package com.delta.esports.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 限流/节流计数器。优先使用 Redis 做分布式计数（多实例共享额度），
 * 未启用 Redis 或 Redis 暂时不可用时回退到单机内存计数，保证功能始终可用。
 */
@Component
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final boolean redisEnabled;

    /** 单机回退：内存计数，过期时间 1 分钟，避免无 Redis 时功能失效 */
    private final Cache<String, Long> fallback = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100000)
            .build();

    public RateLimitService(StringRedisTemplate redis,
                            @Value("${app.rate-limit.redis-enabled:false}") boolean redisEnabled) {
        this.redis = redis;
        this.redisEnabled = redisEnabled;
    }

    /**
     * 尝试计数一次。
     * @param key           计数键
     * @param limit         窗口内允许的最大次数
     * @param windowSeconds 窗口时长（秒）
     * @return true = 放行；false = 超过限制
     */
    public boolean tryAcquire(String key, int limit, long windowSeconds) {
        if (redisEnabled) {
            try {
                Long count = redis.opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redis.expire(key, windowSeconds, TimeUnit.SECONDS);
                }
                return count == null || count <= limit;
            } catch (Exception ignored) {
                // Redis 不可用，落到单机内存限流
            }
        }
        Long current = fallback.getIfPresent(key);
        long next = (current == null ? 0L : current) + 1L;
        fallback.put(key, next);
        return next <= limit;
    }
}
