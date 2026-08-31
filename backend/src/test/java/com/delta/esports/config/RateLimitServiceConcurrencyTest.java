package com.delta.esports.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RateLimitServiceConcurrencyTest {

    @Test
    void localFallbackCountsConcurrentRequestsAtomically() throws Exception {
        int requests = 100;
        int limit = 25;
        RateLimitService service = new RateLimitService(mock(StringRedisTemplate.class), false);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < requests; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return service.tryAcquire("login:same-user", limit, 60);
                }));
            }
            start.countDown();

            int allowed = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) allowed++;
            }
            assertEquals(limit, allowed);
        } finally {
            pool.shutdownNow();
        }
    }
}
