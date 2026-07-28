package com.delta.service;

import com.delta.mapper.OrderMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class OrderTimeoutJobTest {

    @Test
    void cancelsPendingOrdersOlderThanThirtyMinutes() {
        OrderMapper mapper = mock(OrderMapper.class);

        new OrderTimeoutJob(mapper).cancelExpiredOrders();

        verify(mapper).cancelExpiredPending(argThat(cutoff ->
                cutoff.isBefore(LocalDateTime.now().minusMinutes(29)) &&
                cutoff.isAfter(LocalDateTime.now().minusMinutes(31))));
    }
}
