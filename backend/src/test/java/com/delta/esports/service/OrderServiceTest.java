package com.delta.esports.service;

import com.delta.esports.dto.CompleteOrderRequest;
import com.delta.esports.dto.CreateOrderRequest;
import com.delta.esports.entity.*;
import com.delta.esports.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceTest {

    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserMapper userMapper;
    @Autowired private BalanceTransactionMapper balanceTransactionMapper;
    @Autowired private GiftService giftService;
    @Autowired private PaymentOrderMapper paymentOrderMapper;
    @Autowired private SettlementMapper settlementMapper;

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
            booster.setBoosterStatus("idle");
            userMapper.updateById(booster);
        }
    }

    @Test
    void shouldCreateOrderWithoutDeductingBalanceBeforePayment() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");

        BigDecimal balanceBefore = userMapper.selectById(bossId).getBalance();
        com.delta.esports.entity.Order order = orderService.createOrder(bossId, req);

        assertNotNull(order.getId());
        assertEquals("pending_payment", order.getStatus());
        assertNotNull(order.getOrderNo());
        assertEquals(0, balanceBefore.compareTo(userMapper.selectById(bossId).getBalance()));
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

        paymentService.prepare(bossId, orderId, null);
        paymentService.confirmMock(bossId, orderId);
        assertEquals("pending", orderService.getDetail(orderId).getStatus());

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

        Long transferCount = balanceTransactionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getOrderId, orderId)
                        .eq(BalanceTransaction::getType, "TRANSFER"));
        assertTrue(transferCount > 0);
    }

    @Test
    void shouldCancelUnpaidOrderWithoutChangingBalance() {
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
        assertEquals(0, bossBalanceBefore.compareTo(boss.getBalance()));
    }

    @Test
    void shouldAllowCreateWithZeroBalanceBecauseWechatPaymentIsExternal() {
        User boss = userMapper.selectById(bossId);
        boss.setBalance(BigDecimal.ZERO);
        userMapper.updateById(boss);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");

        com.delta.esports.entity.Order order = orderService.createOrder(bossId, req);
        assertEquals("pending_payment", order.getStatus());
    }

    /**
     * 并发测试：两个陪陪同时抢同一单，必须只有一个成功。
     * 修复前：两个请求都先读到 pending，都会把订单改成 assigned（两人都"抢到"）。
     * 修复后：原子 UPDATE ... WHERE status='pending' 保证只有一个生效。
     */
    @Test
    void shouldOnlyOneBoosterClaimTheSameOrder() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");
        com.delta.esports.entity.Order order = orderService.createOrder(bossId, req);
        Long orderId = order.getId();
        // 支付重构后：订单先进入 pending_payment，支付成功后才变为可被抢单的 pending
        paymentService.prepare(bossId, orderId, null);
        paymentService.confirmMock(bossId, orderId);
        assertEquals("pending", orderService.getDetail(orderId).getStatus());

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await(); // 两个线程同时"起跑"
                    orderService.claimOrder(boosterId, orderId);
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 抢单失败是预期行为：另一个线程已经抢走
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, success.get(), "两个陪陪同时抢一单，只能有一个成功");
        assertEquals("assigned", orderService.getDetail(orderId).getStatus());
    }

    @Test
    void shouldPreventOneBoosterFromClaimingTwoOrdersConcurrently() throws Exception {
        com.delta.esports.entity.Order first = createPaidOrder();
        com.delta.esports.entity.Order second = createPaidOrder();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        Future<?> firstClaim = executor.submit(() -> claimAfterSignal(start, success, first.getId()));
        Future<?> secondClaim = executor.submit(() -> claimAfterSignal(start, success, second.getId()));
        start.countDown();
        firstClaim.get(10, TimeUnit.SECONDS);
        secondClaim.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, success.get(), "同一打手不能同时接取两张订单");
        long assigned = List.of(first.getId(), second.getId()).stream()
                .map(orderService::getDetail)
                .filter(order -> "assigned".equals(order.getStatus()))
                .count();
        assertEquals(1, assigned);
        assertEquals("busy", userMapper.selectById(boosterId).getBoosterStatus());
    }

    /**
     * 并发测试：老板余额 100，两个并发请求各送 60 的礼物，只可能成功一个。
     * 修复前：两个请求都读到余额 100，都扣 60，余额变成 -20（或互相覆盖）。
     * 修复后：原子 UPDATE ... AND balance >= 60 保证只有一个扣款成功，余额 40。
     */
    @Test
    void shouldNotOverspendWhenGiftSentConcurrently() throws Exception {
        User boss = userMapper.selectById(bossId);
        boss.setBalance(new BigDecimal("100"));
        userMapper.updateById(boss);

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    giftService.sendGift(bossId, boosterId, "火箭", new BigDecimal("60"), "加油");
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 余额不足被拒绝是预期行为
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, success.get(), "余额 100，两个 60 的礼物并发只能成功一个");
        assertEquals(0, new BigDecimal("40").compareTo(userMapper.selectById(bossId).getBalance()),
                "扣款后余额必须是 40，不能被并发扣两次");
    }

    @Test
    void shouldRejectNonPositiveGiftAmountWithoutChangingBalances() {
        BigDecimal bossBefore = userMapper.selectById(bossId).getBalance();
        BigDecimal boosterBefore = userMapper.selectById(boosterId).getBalance();

        assertThrows(RuntimeException.class, () ->
                giftService.sendGift(bossId, boosterId, "异常礼物", new BigDecimal("-1.00"), null));

        assertEquals(0, bossBefore.compareTo(userMapper.selectById(bossId).getBalance()));
        assertEquals(0, boosterBefore.compareTo(userMapper.selectById(boosterId).getBalance()));
    }

    @Test
    void shouldOnlySettleOnceWhenBossConfirmsConcurrently() throws Exception {
        com.delta.esports.entity.Order order = createSubmittedOrder();
        BigDecimal before = userMapper.selectById(boosterId).getBalance();

        AtomicInteger success = runConcurrently(() -> orderService.bossConfirmOrder(bossId, order.getId()));

        assertEquals(1, success.get());
        assertEquals("done", orderService.getDetail(order.getId()).getStatus());
        assertEquals(0, before.add(order.getAmount())
                .compareTo(userMapper.selectById(boosterId).getBalance()));
        assertEquals(1L, settlementMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Settlement>()
                        .eq(Settlement::getOrderId, order.getId())));
        assertEquals(1L, balanceTransactionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getOrderId, order.getId())
                        .eq(BalanceTransaction::getType, "TRANSFER")));
    }

    @Test
    void shouldOnlyRefundOnceWhenCancelSubmittedConcurrently() throws Exception {
        com.delta.esports.entity.Order order = createPaidOrder();

        AtomicInteger success = runConcurrently(() -> orderService.cancelOrder(bossId, order.getId()));

        assertEquals(1, success.get());
        assertEquals("cancelled", orderService.getDetail(order.getId()).getStatus());
        PaymentOrder payment = paymentOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getOrderId, order.getId()));
        assertEquals("refunded", payment.getStatus());
        assertEquals(0, order.getAmount().compareTo(payment.getRefundedAmount()));
    }

    private com.delta.esports.entity.Order createPaidOrder() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setServiceId(SERVICE_ID);
        req.setGameRegion("国服");
        req.setGameRank("钻石");
        req.setGameMap("长弓溪谷");
        com.delta.esports.entity.Order order = orderService.createOrder(bossId, req);
        paymentService.prepare(bossId, order.getId(), null);
        paymentService.confirmMock(bossId, order.getId());
        return orderService.getDetail(order.getId());
    }

    private com.delta.esports.entity.Order createSubmittedOrder() {
        com.delta.esports.entity.Order order = createPaidOrder();
        orderService.claimOrder(boosterId, order.getId());
        orderService.startOrder(boosterId, order.getId());
        CompleteOrderRequest request = new CompleteOrderRequest();
        request.setOrderId(order.getId());
        request.setIsQualified(true);
        orderService.submitOrder(boosterId, request);
        return orderService.getDetail(order.getId());
    }

    private AtomicInteger runConcurrently(Runnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                try {
                    start.await();
                    action.run();
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 并发竞争失败的一方应收到业务冲突。
                }
            }));
        }
        start.countDown();
        for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS);
        executor.shutdown();
        return success;
    }

    private void claimAfterSignal(CountDownLatch start, AtomicInteger success, Long orderId) {
        try {
            start.await();
            orderService.claimOrder(boosterId, orderId);
            success.incrementAndGet();
        } catch (Exception ignored) {
            // 其中一笔订单应因打手已被原子占用而失败。
        }
    }
}
