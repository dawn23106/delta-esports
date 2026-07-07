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

    public Order getDetail(Long id) {
        return orderMapper.selectById(id);
    }

    // ==================== 创建订单（冻结资金） ====================

    @Transactional
    public Order createOrder(Long bossId, CreateOrderRequest req) {
        ServiceItem service = serviceItemMapper.selectById(req.getServiceId());
        if (service == null || service.getIsActive() != 1) {
            throw new BusinessException("服务项目不存在或已下架");
        }

        // 校验余额
        User boss = userMapper.selectById(bossId);
        BigDecimal amount = service.getBasePrice();
        if (boss.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(400, "余额不足");
        }

        // 冻结资金
        boss.setBalance(boss.getBalance().subtract(amount));
        userMapper.updateById(boss);

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBossId(bossId);
        order.setServiceId(service.getId());
        order.setServiceName(service.getName());
        order.setAmount(amount);
        order.setStatus("pending");
        order.setGameMap(req.getGameMap());
        order.setGameRegion(req.getGameRegion());
        order.setGameRank(req.getGameRank());
        order.setBossNote(req.getBossNote());

        if (req.getBoosterId() != null) {
            order.setBoosterId(req.getBoosterId());
            order.setStatus("assigned");
        }

        orderMapper.insert(order);

        // 记录冻结流水
        recordTransaction(bossId, order.getId(), amount.negate(), "FREEZE",
                boss.getBalance(), "订单创建，冻结资金");

        return order;
    }

    // ==================== 接单 ====================

    @Transactional
    public Order claimOrder(Long boosterId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"pending".equals(order.getStatus())) throw new BusinessException("订单已被接取");

        order.setBoosterId(boosterId);
        order.setStatus("assigned");
        orderMapper.updateById(order);
        return order;
    }

    // ==================== 开始服务 ====================

    @Transactional
    public Order startOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"assigned".equals(order.getStatus())) throw new BusinessException("订单状态不正确");
        if (!order.getBoosterId().equals(userId)) throw new BusinessException("无权操作");

        order.setStatus("in_progress");
        orderMapper.updateById(order);
        return order;
    }

    // ==================== 陪陪提交成果 ====================

    @Transactional
    public Order submitOrder(Long boosterId, CompleteOrderRequest req) {
        Order order = orderMapper.selectById(req.getOrderId());
        if (order == null) throw new BusinessException("订单不存在");
        if (!"in_progress".equals(order.getStatus())) throw new BusinessException("订单状态不正确");
        if (!order.getBoosterId().equals(boosterId)) throw new BusinessException("无权操作");

        order.setStatus("submitted");
        order.setIsQualified(req.getIsQualified() != null && req.getIsQualified() ? 1 : 0);
        order.setResultNote(req.getResultNote());
        order.setResultImages(req.getResultImages());
        orderMapper.updateById(order);
        return order;
    }

    // ==================== 老板确认完成 ====================

    @Transactional
    public Order bossConfirmOrder(Long bossId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"submitted".equals(order.getStatus())) throw new BusinessException("订单状态不正确，请等待陪陪提交成果");
        if (!order.getBossId().equals(bossId)) throw new BusinessException("无权操作");

        // 转账给陪陪
        User booster = userMapper.selectById(order.getBoosterId());
        booster.setBalance(booster.getBalance().add(order.getAmount()));
        userMapper.updateById(booster);

        recordTransaction(booster.getId(), orderId, order.getAmount(), "TRANSFER",
                booster.getBalance(), "订单完成，收入到账");

        // 创建结算记录
        Settlement settlement = new Settlement();
        settlement.setOrderId(order.getId());
        settlement.setBoosterId(order.getBoosterId());
        settlement.setAmount(order.getAmount());
        settlement.setStatus("completed");
        settlementMapper.insert(settlement);

        // 更新陪陪统计
        updateBoosterStats(order.getBoosterId());

        order.setStatus("done");
        orderMapper.updateById(order);
        return order;
    }

    // ==================== 取消订单（退款） ====================

    @Transactional
    public Order cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"pending".equals(order.getStatus()) && !"assigned".equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }
        if (!order.getBossId().equals(userId) && !order.getBoosterId().equals(userId)) {
            throw new BusinessException("无权操作");
        }

        // 退款给老板
        User boss = userMapper.selectById(order.getBossId());
        boss.setBalance(boss.getBalance().add(order.getAmount()));
        userMapper.updateById(boss);

        recordTransaction(boss.getId(), orderId, order.getAmount(), "REFUND",
                boss.getBalance(), "订单取消，退款");

        order.setStatus("cancelled");
        orderMapper.updateById(order);
        return order;
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
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"pending".equals(order.getStatus())) throw new BusinessException("订单状态不允许派单");

        order.setBoosterId(boosterId);
        order.setStatus("assigned");
        orderMapper.updateById(order);
        return order;
    }

    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"completed".equals(order.getStatus())) throw new BusinessException("订单状态不允许确认");

        order.setStatus("done");
        orderMapper.updateById(order);
        return order;
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

    private void updateBoosterStats(Long boosterId) {
        User booster = userMapper.selectById(boosterId);
        if (booster == null) return;

        booster.setTotalOrders((booster.getTotalOrders() == null ? 0 : booster.getTotalOrders()) + 1);

        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getBoosterId, boosterId));
        if (!reviews.isEmpty()) {
            double avgRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(5.0);
            booster.setRating(BigDecimal.valueOf(Math.round(avgRating * 100.0) / 100.0));
        }

        userMapper.updateById(booster);
    }
}
