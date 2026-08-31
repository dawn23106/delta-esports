package com.delta.esports.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheTemplateLockTest {

    @SuppressWarnings("unchecked")
    @Test
    void releasesOnlyWithAcquiredOwnerToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq("delta:lock:cleanup"), any(String.class),
                eq(120L), eq(TimeUnit.SECONDS))).thenReturn(true);
        RedisCacheTemplate cache = new RedisCacheTemplate(redis, new ObjectMapper());

        String token = cache.acquireLock("cleanup", 120);
        assertNotNull(token);
        assertFalse(token.isBlank());
        cache.releaseLock("cleanup", token);

        verify(redis).execute(any(), eq(java.util.List.of("delta:lock:cleanup")), eq(token));
    }
}
