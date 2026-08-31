package com.delta.esports.push;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderPushWebSocketHandlerConcurrencyTest {

    @Test
    void serializesConcurrentSendsToSameSession() throws Exception {
        OrderPushWebSocketHandler handler = new OrderPushWebSocketHandler();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userId", 7L));
        when(session.isOpen()).thenReturn(true);

        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        AtomicInteger sends = new AtomicInteger();
        doAnswer(invocation -> {
            int current = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(30);
                sends.incrementAndGet();
                return null;
            } finally {
                inFlight.decrementAndGet();
            }
        }).when(session).sendMessage(any());

        handler.afterConnectionEstablished(session);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            pool.submit(() -> pushAfterStart(handler, start, "first"));
            pool.submit(() -> pushAfterStart(handler, start, "second"));
            start.countDown();
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertEquals(2, sends.get());
        assertEquals(1, maxInFlight.get());
    }

    private void pushAfterStart(OrderPushWebSocketHandler handler, CountDownLatch start, String value) {
        try {
            start.await();
            handler.pushToUserLocal(7L, "message", value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
