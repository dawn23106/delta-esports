package com.delta.esports.config;

import com.delta.esports.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单超时清理：周期扫描超过指定时间仍未支付的订单并取消，避免待支付订单无限堆积。
 * 多实例部署时用 Redis 分布式锁保证同一时刻只有一个实例执行扫描，避免重复劳动。
 */
@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);
    private static final String CLEANUP_LOCK_KEY = "order-cleanup";
    /** 锁持有时间：任务 60s 一次，120s 足够且能容忍任务短暂卡顿 */
    private static final long CLEANUP_LOCK_TTL_SECONDS = 120;

    private final PaymentService paymentService;
    private final RedisCacheTemplate cache;

    @Value("${app.order-timeout.minutes:30}")
    private int timeoutMinutes;

    public OrderTimeoutScheduler(PaymentService paymentService, RedisCacheTemplate cache) {
        this.paymentService = paymentService;
        this.cache = cache;
    }

    @Scheduled(fixedDelayString = "${app.order-timeout.scan-ms:60000}",
            initialDelayString = "${app.order-timeout.initial-delay-ms:60000}")
    public void cancelStaleUnpaidOrders() {
        if (!cache.tryLock(CLEANUP_LOCK_KEY, CLEANUP_LOCK_TTL_SECONDS)) {
            return; // 已有其它实例在跑本轮清理
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
            int cancelled = paymentService.cancelStaleUnpaidOrders(deadline);
            if (cancelled > 0) {
                log.info("Order timeout cleanup: cancelled {} stale unpaid order(s)", cancelled);
            }
        } finally {
            cache.unlock(CLEANUP_LOCK_KEY);
        }
    }
}
