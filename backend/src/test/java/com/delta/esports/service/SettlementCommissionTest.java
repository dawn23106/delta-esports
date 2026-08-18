package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.dto.CompleteOrderRequest;
import com.delta.esports.dto.CreateOrderRequest;
import com.delta.esports.entity.Order;
import com.delta.esports.entity.Settlement;
import com.delta.esports.entity.User;
import com.delta.esports.mapper.SettlementMapper;
import com.delta.esports.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = "app.commission.rate=0.15")
class SettlementCommissionTest {

    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserMapper userMapper;
    @Autowired private SettlementMapper settlementMapper;

    private Long bossId;
    private Long boosterId;

    @BeforeEach
    void setUp() {
        bossId = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, "13900000001")).getId();
        boosterId = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, "13600000001")).getId();
        User booster = userMapper.selectById(boosterId);
        booster.setBoosterStatus("idle");
        userMapper.updateById(booster);
    }

    @Test
    void shouldSplitSettlementByCommissionRate() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(1L);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");
        Order order = orderService.createOrder(bossId, req);
        paymentService.prepare(bossId, order.getId(), null);
        paymentService.confirmMock(bossId, order.getId());
        orderService.claimOrder(boosterId, order.getId());
        orderService.startOrder(boosterId, order.getId());

        CompleteOrderRequest complete = new CompleteOrderRequest();
        complete.setOrderId(order.getId());
        complete.setIsQualified(true);
        complete.setResultNote("完成");
        orderService.submitOrder(boosterId, complete);

        BigDecimal balanceBefore = userMapper.selectById(boosterId).getBalance();
        orderService.bossConfirmOrder(bossId, order.getId());

        BigDecimal gross = order.getAmount();
        BigDecimal expectedNet = gross.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedCommission = gross.subtract(expectedNet);

        BigDecimal balanceAfter = userMapper.selectById(boosterId).getBalance();
        assertEquals(0, balanceBefore.add(expectedNet).compareTo(balanceAfter), "打手应只拿到净收入");

        Settlement settlement = settlementMapper.selectOne(
                new LambdaQueryWrapper<Settlement>().eq(Settlement::getOrderId, order.getId()));
        assertEquals(0, gross.compareTo(settlement.getAmount()), "结算应记录订单总额");
        assertEquals(0, expectedNet.compareTo(settlement.getNetAmount()), "结算应记录净收入");
        assertEquals(0, expectedCommission.compareTo(settlement.getCommission()), "结算应记录平台抽成");
        assertEquals(0, new BigDecimal("0.1500").compareTo(settlement.getCommissionRate()), "结算应记录抽成比例");
    }
}
