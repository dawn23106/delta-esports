package com.delta.service;

import com.delta.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Database-backed timeout job; safe across restarts and independent of Redis key events. */
@Component
public class OrderTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutJob.class);
    private final OrderMapper orderMapper;

    public OrderTimeoutJob(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Scheduled(fixedDelayString = "${app.order-timeout-scan-ms:60000}")
    public void cancelExpiredOrders() {
        int updated = orderMapper.cancelExpiredPending(LocalDateTime.now().minusMinutes(30));
        if (updated > 0) {
            log.info("Cancelled {} expired pending order(s)", updated);
        }
    }
}
