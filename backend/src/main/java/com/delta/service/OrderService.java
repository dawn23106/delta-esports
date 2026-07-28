package com.delta.service;

import com.delta.common.BusinessException;
import com.delta.common.ErrorCode;
import com.delta.common.PageResult;
import com.delta.common.SensitiveWordFilter;
import com.delta.entity.Order;
import com.delta.mapper.OrderMapper;
import com.delta.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * 订单核心服务 — 管理订单全生命周期。
 *
 * 状态流转: pending → assigned → in_progress → completed / cancelled
 *
 * 核心机制：
 *   乐观锁抢单 — UPDATE WHERE status='pending'，受影响行数=0说明已被抢
 *   Redis SETNX防重复 — 同一用户同一游戏5秒内只能下一次单
 *   Redis TTL超时 — 订单30分钟无人接单自动取消
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private StringRedisTemplate redis;

    public OrderService(OrderMapper orderMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    @Autowired(required = false)
    public void setRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 玩家下单 — 创建订单，初始状态 pending。
     * 包含敏感词过滤和防重复下单两个安全检查。
     */
    public Order create(Long customerId, String game, String serviceType, String detail,
                        BigDecimal price, String sourceChannel) {
        // 敏感词过滤 — 阻止飞单（私下交易）、外挂、赌博等
        if (SensitiveWordFilter.hasSensitive(detail)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "订单描述包含敏感词: " + SensitiveWordFilter.getFirstMatch(detail));
        }

        // Redis SETNX 防重复 — 同一用户+同一游戏5秒内只能下一单
        if (redis != null) {
            String lockKey = "order:lock:" + customerId + ":" + game;
            Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
            if (locked == null || !locked) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "操作过于频繁，请5秒后再试");
            }
        }

        Order order = new Order();
        order.setGame(game);
        order.setServiceType(serviceType != null ? serviceType : "tech");
        order.setDetail(detail);
        order.setPrice(price);
        order.setStatus("pending");
        order.setCustomerId(customerId);
        order.setSourceChannel(sourceChannel);
        orderMapper.insert(order);

        // 30分钟超时机制 — Redis key过期后通过监听自动取消订单
        if (redis != null) {
            String timeoutKey = "order:timeout:" + order.getId();
            redis.opsForValue().set(timeoutKey, "pending", Duration.ofMinutes(30));
        }

        return orderMapper.findById(order.getId());
    }

    /**
     * 打手抢单 — 乐观锁实现。
     * 先检查角色、订单状态和存在性，再执行 UPDATE WHERE status='pending'，
     * 如果 affected=0 说明并发环境中订单已被别人抢走。
     */
    public Order claim(Long orderId, Long boosterId) {
        var booster = userMapper.findById(boosterId);
        if (booster == null || !Boolean.TRUE.equals(booster.getIsActive()) || !"booster".equals(booster.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅打手可接单");
        }
        Order order = orderMapper.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许接单");
        }
        // 关键：乐观锁 — 只更新 status='pending' 的行，并发抢单时只有一个人成功
        int affected = orderMapper.claimOptimistic(orderId, boosterId);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单已被他人接走");
        }
        return orderMapper.findById(orderId);
    }

    /** 打手开始代练 — 校验归属权，状态 assigned → in_progress */
    public Order start(Long orderId, Long boosterId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        if (!boosterId.equals(order.getBoosterId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "这不是您的订单");
        }
        if (!"assigned".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许开始");
        }
        orderMapper.updateStatus(orderId, "in_progress");
        return orderMapper.findById(orderId);
    }

    /** 打手提交完成 — 校验归属权，状态 in_progress → completed */
    public Order complete(Long orderId, Long boosterId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        if (!boosterId.equals(order.getBoosterId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "这不是您的订单");
        }
        if (!"in_progress".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许完成");
        }
        orderMapper.updateStatus(orderId, "completed");
        return orderMapper.findById(orderId);
    }

    /** 取消订单 — 玩家只能取消自己的，客服可取消任意订单。终态订单不可取消。 */
    public Order cancel(Long orderId, Long userId, String role) {
        Order order = orderMapper.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        if (!"cs".equals(role) && !userId.equals(order.getCustomerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权取消此订单");
        }
        if ("completed".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单已结束，无法取消");
        }
        orderMapper.updateStatus(orderId, "cancelled");
        return orderMapper.findById(orderId);
    }

    /** 玩家查看自己的订单（分页） */
    public PageResult<Order> findMyOrders(Long customerId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findByCustomer(customerId, offset, pageSize);
        long total = orderMapper.countByCustomer(customerId);
        return PageResult.of(list, total, page, pageSize);
    }

    /** 打手浏览可接订单池 — 只查 pending 状态 */
    public PageResult<Order> findPool(String game, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findPool(null, game, offset, pageSize);
        long total = orderMapper.countPool(game);
        return PageResult.of(list, total, page, pageSize);
    }

    /** 打手查看自己已接的订单 */
    public PageResult<Order> findBoosterOrders(Long boosterId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findByBooster(boosterId, offset, pageSize);
        long total = orderMapper.countByBooster(boosterId);
        return PageResult.of(list, total, page, pageSize);
    }

    /** 客服查看全量订单 — 支持状态和游戏筛选 */
    public PageResult<Order> findAllOrders(String status, String game, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findAll(status, game, offset, pageSize);
        long total = orderMapper.countAll(status, game);
        return PageResult.of(list, total, page, pageSize);
    }

    /** 客服派单 — 跳过抢单，直接把 pending 订单分配给指定打手 */
    public Order assign(Long orderId, Long boosterId, Long csId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待接单状态的订单才能派单");
        }
        var booster = userMapper.findById(boosterId);
        if (booster == null || !Boolean.TRUE.equals(booster.getIsActive()) || !"booster".equals(booster.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指定用户不是可用服务者");
        }
        if (orderMapper.assign(orderId, boosterId, csId) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单状态已变化，请刷新后重试");
        }
        return orderMapper.findById(orderId);
    }

    /** 客服直接创建订单 — 状态直接 assigned，跳过抢单 */
    public Order createByCs(Long customerId, Long boosterId, Long csId,
                            String game, String detail, BigDecimal price) {
        var customer = userMapper.findById(customerId);
        var booster = userMapper.findById(boosterId);
        if (customer == null || !Boolean.TRUE.equals(customer.getIsActive())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "玩家不存在或已停用");
        }
        if (booster == null || !Boolean.TRUE.equals(booster.getIsActive()) || !"booster".equals(booster.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指定用户不是可用服务者");
        }
        Order order = new Order();
        order.setGame(game);
        order.setDetail(detail);
        order.setPrice(price);
        order.setStatus("assigned");
        order.setCustomerId(customerId);
        order.setBoosterId(boosterId);
        order.setCsId(csId);
        orderMapper.insert(order);
        return orderMapper.findById(order.getId());
    }
}
