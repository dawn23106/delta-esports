package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.dto.CompleteOrderRequest;
import com.delta.esports.dto.CreateOrderRequest;
import com.delta.esports.entity.Order;
import com.delta.esports.entity.User;
import com.delta.esports.entity.Withdrawal;
import com.delta.esports.mapper.UserMapper;
import com.delta.esports.mapper.WithdrawalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
// commission=0 让净收入等于订单额；lock-days=0 让结算立即可提，便于聚焦提现流程本身
@TestPropertySource(properties = {"app.commission.rate=0", "app.withdrawal.lock-days=0"})
class WithdrawalServiceTest {

    @Autowired private WithdrawalService withdrawalService;
    @Autowired private WithdrawalMapper withdrawalMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;

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
    void shouldApplyAndRejectWithdrawal() {
        completeOrder();

        BigDecimal before = userMapper.selectById(boosterId).getBalance();

        Withdrawal w = withdrawalService.apply(boosterId, new BigDecimal("50.00"));
        assertEquals("pending", w.getStatus());
        assertEquals(0, before.subtract(new BigDecimal("50.00")).compareTo(userMapper.selectById(boosterId).getBalance()),
                "申请提现应立即冻结余额");

        Withdrawal rejected = withdrawalService.review(w.getId(), "reject", "测试驳回");
        assertEquals("rejected", rejected.getStatus());
        assertEquals(0, before.compareTo(userMapper.selectById(boosterId).getBalance()), "驳回后应退回冻结余额");
    }

    @Test
    void shouldRejectOverWithdrawal() {
        completeOrder();

        BigDecimal available = userMapper.selectById(boosterId).getBalance();
        assertThrows(BusinessException.class,
                () -> withdrawalService.apply(boosterId, available.add(new BigDecimal("1.00"))),
                "超出可提现金额应被拒绝");
    }

    private void completeOrder() {
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
        orderService.bossConfirmOrder(bossId, order.getId());
    }
}
