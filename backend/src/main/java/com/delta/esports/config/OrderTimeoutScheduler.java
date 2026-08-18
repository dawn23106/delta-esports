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
 */
@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    private final PaymentService paymentService;

    @Value("${app.order-timeout.minutes:30}")
    private int timeoutMinutes;

    public OrderTimeoutScheduler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelayString = "${app.order-timeout.scan-ms:60000}",
            initialDelayString = "${app.order-timeout.initial-delay-ms:60000}")
    public void cancelStaleUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        int cancelled = paymentService.cancelStaleUnpaidOrders(deadline);
        if (cancelled > 0) {
            log.info("Order timeout cleanup: cancelled {} stale unpaid order(s)", cancelled);
        }
    }
}
