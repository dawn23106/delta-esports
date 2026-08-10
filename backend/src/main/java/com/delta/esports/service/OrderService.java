package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.dto.CompleteOrderRequest;
import com.delta.esports.dto.CreateOrderRequest;
import com.delta.esports.entity.*;
import com.delta.esports.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private SettlementMapper settlementMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private BalanceTransactionMapper balanceTransactionMapper;
    @Autowired
    private PaymentService paymentService;

    // ==================== 查询 ====================

    public Page<Order> getPool(int page, int size) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        qw.eq(Order::getStatus, "pending").orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), qw);
    }

    public Page<Order> getMyOrders(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.eq(Order::getBossId, userId).or().eq(Order::getBoosterId, userId));
        if (status != null && !status.isEmpty()) {
            qw.eq(Order::getStatus, status);
        }
        qw.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), qw);
    }

    public Order getDetail(Long id, Long userId, String role) {
        Order order = orderMapper.selectById(id);
        if (order == null) return null;
        boolean participant = userId.equals(order.getBossId()) || userId.equals(order.getBoosterId());
        boolean visiblePoolOrder = "booster".equals(role) && "pending".equals(order.getStatus());
        if (!participant && !visiblePoolOrder && !"admin".equals(role)) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return order;
    }

    /** Internal lookup used by service-level workflows and tests. Controllers must use the permission-aware overload. */
    public Order getDetail(Long id) {
        return orderMapper.selectById(id);
    }

    // ==================== 创建订单（等待在线支付） ====================

    @Transactional
    public Order createOrder(Long bossId, CreateOrderRequest req) {
        ServiceItem service = serviceItemMapper.selectById(req.getServiceId());
        if (service == null || service.getIsActive() != 1) {
            throw new BusinessException("服务项目不存在或已下架");
        }

        BigDecimal amount = service.getBasePrice();

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBossId(bossId);
        order.setServiceId(service.getId());
        order.setServiceName(service.getName());
        order.setAmount(amount);
        order.setStatus("pending_payment");
        order.setGameMap(req.getGameMap());
        order.setGameRegion(req.getGameRegion());
        order.setGameRank(req.getGameRank());
        order.setBossNote(req.getBossNote());

        if (req.getBoosterId() != null) {
            requireActiveBooster(req.getBoosterId());
            order.setBoosterId(req.getBoosterId());
        }

        orderMapper.insert(order);
        return order;
    }

    // ==================== 接单 ====================

    @Transactional
    public Order claimOrder(Long boosterId, Long orderId) {
        if (userMapper.reserveBooster(boosterId) == 0) {
            throw new BusinessException("当前不是空闲接单状态或已有进行中的订单");
        }
        // 原子抢单：只有订单状态还是 pending 时，这次更新才成功（返回 1）。
        // 两个陪陪同时抢单：数据库只让一个请求的 UPDATE 生效，另一个返回 0 直接失败。
        if (orderMapper.claim(orderId, boosterId) == 0) {
            throw new BusinessException("订单已被接取");
        }
        return orderMapper.selectById(orderId);
    }

    // ==================== 开始服务 ====================

    @Transactional
    public Order startOrder(Long userId, Long orderId) {
        if (orderMapper.start(orderId, userId) == 0) {
            throw new BusinessException("订单状态已变化或无权操作");
        }
        return orderMapper.selectById(orderId);
    }

    // ==================== 陪陪提交成果 ====================

    @Transactional
    public Order submitOrder(Long boosterId, CompleteOrderRequest req) {
        int qualified = Boolean.TRUE.equals(req.getIsQualified()) ? 1 : 0;
        if (orderMapper.submit(req.getOrderId(), boosterId, qualified,
                req.getResultNote(), req.getResultImages()) == 0) {
            throw new BusinessException("订单状态已变化或无权操作");
        }
        return orderMapper.selectById(req.getOrderId());
    }

    // ==================== 老板确认完成 ====================

    @Transactional
    public Order bossConfirmOrder(Long bossId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"submitted".equals(order.getStatus())) throw new BusinessException("订单状态不正确，请等待陪陪提交成果");
        if (!order.getBossId().equals(bossId)) throw new BusinessException("无权操作");

        return finalizeSubmittedOrder(order);
    }

    private Order finalizeSubmittedOrder(Order order) {
        Long orderId = order.getId();
        // 第一步：原子抢占“完成”资格 —— 只有状态还是 submitted 才更新成功（返回 1）。
        // 这一步是并发防线：两个请求同时点确认，只有一个能通过，另一个返回 0。
        if (orderMapper.markDone(orderId) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        // 第二步：抢占成功后才动钱 —— 只有通过上面的请求才有资格转账，不会重复转账。
        // 原子转账给陪陪：数据库自己算 balance = balance + amount，不依赖读到的旧值。
        userMapper.addBalance(order.getBoosterId(), order.getAmount());
        // 完成单数原子 +1。
        userMapper.incrementTotalOrders(order.getBoosterId());
        userMapper.releaseBooster(order.getBoosterId());

        // 创建结算记录
        Settlement settlement = new Settlement();
        settlement.setOrderId(order.getId());
        settlement.setBoosterId(order.getBoosterId());
        settlement.setAmount(order.getAmount());
        settlement.setStatus("completed");
        settlementMapper.insert(settlement);

        // 记录转账流水（转账后重新读一次余额，balance_after 才准确）
        User booster = userMapper.selectById(order.getBoosterId());
        recordTransaction(booster.getId(), orderId, order.getAmount(), "TRANSFER",
                booster.getBalance(), "订单完成，收入到账");

        // 更新陪陪评分
        updateBoosterRating(order.getBoosterId());

        return orderMapper.selectById(orderId);
    }

    // ==================== 取消订单（退款） ====================

    @Transactional
    public Order cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"pending_payment".equals(order.getStatus())
                && !"pending".equals(order.getStatus())
                && !"assigned".equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }
        if (!order.getBossId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        Order result = paymentService.cancelAndRefund(order);
        if (order.getBoosterId() != null) userMapper.releaseBooster(order.getBoosterId());
        return result;
    }

    // ==================== Admin 操作 ====================

    public Page<Order> adminPage(int page, int size, String status) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            qw.eq(Order::getStatus, status);
        }
        qw.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), qw);
    }

    @Transactional
    public Order assignOrder(Long orderId, Long boosterId) {
        if (userMapper.reserveBooster(boosterId) == 0) {
            throw new BusinessException("指定陪陪当前不可接单");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        // 和抢单同一个原子操作：防止管理员连续派单，两个请求只有一个生效。
        if (orderMapper.claim(orderId, boosterId) == 0) {
            throw new BusinessException("订单状态不允许派单");
        }
        return orderMapper.selectById(orderId);
    }

    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"submitted".equals(order.getStatus())) throw new BusinessException("订单状态不允许确认");
        return finalizeSubmittedOrder(order);
    }

    @Transactional
    public Order disputeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        order.setStatus("disputed");
        orderMapper.updateById(order);
        return order;
    }

    // ==================== 私有方法 ====================

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "DE" + datePart + uuidPart;
    }

    private void recordTransaction(Long userId, Long orderId, BigDecimal amount,
                                   String type, BigDecimal balanceAfter, String remark) {
        BalanceTransaction tx = new BalanceTransaction();
        tx.setUserId(userId);
        tx.setOrderId(orderId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setBalanceAfter(balanceAfter);
        tx.setRemark(remark);
        balanceTransactionMapper.insert(tx);
    }

    /**
     * 更新陪陪评分。
     * 注意：只用一个只带 id + rating 的新对象去 updateById，
     * 绝不能用刚用原子 SQL 加过余额的旧对象整块写回，否则会把新余额覆盖成旧值。
     */
    private void updateBoosterRating(Long boosterId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getBoosterId, boosterId));
        if (reviews.isEmpty()) return;

        double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(5.0);

        User update = new User();
        update.setId(boosterId);
        update.setRating(BigDecimal.valueOf(Math.round(avgRating * 100.0) / 100.0));
        userMapper.updateById(update);
    }

    private void requireActiveBooster(Long boosterId) {
        User booster = userMapper.selectById(boosterId);
        if (booster == null || !"booster".equals(booster.getRole())
                || "banned".equals(booster.getStatus())) {
            throw new BusinessException(400, "指定陪陪不存在或不可接单");
        }
    }
}
