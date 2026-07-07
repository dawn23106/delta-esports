package com.delta.esports.service;

import com.delta.esports.dto.CompleteOrderRequest;
import com.delta.esports.dto.CreateOrderRequest;
import com.delta.esports.entity.*;
import com.delta.esports.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceTest {

    @Autowired private OrderService orderService;
    @Autowired private UserMapper userMapper;
    @Autowired private BalanceTransactionMapper balanceTransactionMapper;

    private Long bossId;
    private Long boosterId;
    private static final Long SERVICE_ID = 1L;

    @BeforeEach
    void setUp() {
        User boss = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, "13900000001"));
        if (boss != null) {
            bossId = boss.getId();
            boss.setBalance(new BigDecimal("5000"));
            userMapper.updateById(boss);
        }
        User booster = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, "13600000001"));
        if (booster != null) {
            boosterId = booster.getId();
        }
    }

    @Test
    void shouldCreateOrderAndFreezeBalance() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");

        com.delta.esports.entity.Order order = orderService.createOrder(bossId, req);

        assertNotNull(order.getId());
        assertEquals("pending", order.getStatus());
        assertNotNull(order.getOrderNo());

        User boss = userMapper.selectById(bossId);
        assertEquals(0, new BigDecimal("5000").subtract(order.getAmount()).compareTo(boss.getBalance()));
    }

    @Test
    void shouldCreateClaimStartSubmitConfirmFlow() {
        // Create
        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");
        com.delta.esports.entity.Order order = orderService.createOrder(bossId, req);
        Long orderId = order.getId();

        // Claim
        order = orderService.claimOrder(boosterId, orderId);
        assertEquals("assigned", order.getStatus());

        // Start
        order = orderService.startOrder(boosterId, orderId);
        assertEquals("in_progress", order.getStatus());

        // Submit
        CompleteOrderRequest completeReq = new CompleteOrderRequest();
        completeReq.setOrderId(orderId);
        completeReq.setIsQualified(true);
        completeReq.setResultNote("任务完成");
        order = orderService.submitOrder(boosterId, completeReq);
        assertEquals("submitted", order.getStatus());

        // Boss confirm — transfer money
        BigDecimal boosterBalanceBefore = userMapper.selectById(boosterId).getBalance();
        order = orderService.bossConfirmOrder(bossId, orderId);
        assertEquals("done", order.getStatus());

        User booster = userMapper.selectById(boosterId);
        assertEquals(0, boosterBalanceBefore.add(order.getAmount()).compareTo(booster.getBalance()));
        assertTrue(booster.getTotalOrders() > 0);

        // Verify FREEZE + TRANSFER transactions
        Long freezeCount = balanceTransactionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getOrderId, orderId)
                        .eq(BalanceTransaction::getType, "FREEZE"));
        assertTrue(freezeCount > 0);

        Long transferCount = balanceTransactionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getOrderId, orderId)
                        .eq(BalanceTransaction::getType, "TRANSFER"));
        assertTrue(transferCount > 0);
    }

    @Test
    void shouldCancelOrderAndRefund() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国际服");
        req.setGameRank("黄金");
        req.setGameMap("零号大坝");
        com.delta.esports.entity.Order order = orderService.createOrder(bossId, req);

        BigDecimal bossBalanceBefore = userMapper.selectById(bossId).getBalance();
        com.delta.esports.entity.Order cancelled = orderService.cancelOrder(bossId, order.getId());
        assertEquals("cancelled", cancelled.getStatus());

        User boss = userMapper.selectById(bossId);
        assertEquals(0, bossBalanceBefore.add(cancelled.getAmount()).compareTo(boss.getBalance()));

        Long refundCount = balanceTransactionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getOrderId, order.getId())
                        .eq(BalanceTransaction::getType, "REFUND"));
        assertTrue(refundCount > 0);
    }

    @Test
    void shouldRejectCreateWithInsufficientBalance() {
        User boss = userMapper.selectById(bossId);
        boss.setBalance(BigDecimal.ZERO);
        userMapper.updateById(boss);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");

        assertThrows(Exception.class, () -> orderService.createOrder(bossId, req));
    }
}
