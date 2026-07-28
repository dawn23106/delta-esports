package com.delta.service;

import com.delta.common.BusinessException;
import com.delta.common.ErrorCode;
import com.delta.entity.Order;
import com.delta.entity.User;
import com.delta.mapper.OrderMapper;
import com.delta.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderMapper orderMapper;
    private UserMapper userMapper;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        userMapper = mock(UserMapper.class);
        service = new OrderService(orderMapper, userMapper);
    }

    @Test
    void claimUsesConditionalUpdateAndReturnsWinner() {
        User booster = user(2L, "booster", true);
        Order pending = order(10L, "pending", null);
        Order assigned = order(10L, "assigned", 2L);
        when(userMapper.findById(2L)).thenReturn(booster);
        when(orderMapper.findById(10L)).thenReturn(pending, assigned);
        when(orderMapper.claimOptimistic(10L, 2L)).thenReturn(1);

        Order result = service.claim(10L, 2L);

        assertEquals("assigned", result.getStatus());
        assertEquals(2L, result.getBoosterId());
        verify(orderMapper).claimOptimistic(10L, 2L);
    }

    @Test
    void claimReportsConflictWhenAnotherRequestWon() {
        when(userMapper.findById(2L)).thenReturn(user(2L, "booster", true));
        when(orderMapper.findById(10L)).thenReturn(order(10L, "pending", null));
        when(orderMapper.claimOptimistic(10L, 2L)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.claim(10L, 2L));

        assertEquals(ErrorCode.CONFLICT.code, error.getCode());
    }

    @Test
    void claimRejectsInactiveOrWrongRoleUser() {
        when(userMapper.findById(2L)).thenReturn(user(2L, "player", true));

        BusinessException error = assertThrows(BusinessException.class, () -> service.claim(10L, 2L));

        assertEquals(ErrorCode.FORBIDDEN.code, error.getCode());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void assignValidatesBoosterAndConditionalUpdate() {
        when(orderMapper.findById(10L)).thenReturn(order(10L, "pending", null));
        when(userMapper.findById(2L)).thenReturn(user(2L, "booster", true));
        when(orderMapper.assign(10L, 2L, 3L)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.assign(10L, 2L, 3L));

        assertEquals(ErrorCode.CONFLICT.code, error.getCode());
    }

    private static User user(Long id, String role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setIsActive(active);
        return user;
    }

    private static Order order(Long id, String status, Long boosterId) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setBoosterId(boosterId);
        return order;
    }
}
