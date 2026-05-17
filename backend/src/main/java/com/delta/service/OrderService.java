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

    public Order create(Long customerId, String game, String serviceType, String detail,
                        BigDecimal price, String sourceChannel) {
        // 敏感词过滤
        if (SensitiveWordFilter.hasSensitive(detail)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "订单描述包含敏感词: " + SensitiveWordFilter.getFirstMatch(detail));
        }

        // Redis 防重复
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

        // 30分钟超时自动取消 (Redis 延迟队列)
        if (redis != null) {
            String timeoutKey = "order:timeout:" + order.getId();
            redis.opsForValue().set(timeoutKey, "pending",
                    Duration.ofMinutes(30));
        }

        return orderMapper.findById(order.getId());
    }

    public Order claim(Long orderId, Long boosterId) {
        var booster = userMapper.findById(boosterId);
        if (!"booster".equals(booster.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅打手可接单");
        }
        Order order = orderMapper.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许接单");
        }
        int affected = orderMapper.claimOptimistic(orderId, boosterId);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单已被他人接走");
        }
        return orderMapper.findById(orderId);
    }

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

    public PageResult<Order> findMyOrders(Long customerId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findByCustomer(customerId, offset, pageSize);
        long total = orderMapper.countByCustomer(customerId);
        return PageResult.of(list, total, page, pageSize);
    }

    public PageResult<Order> findPool(String game, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findPool(null, game, offset, pageSize);
        long total = orderMapper.countPool(game);
        return PageResult.of(list, total, page, pageSize);
    }

    public PageResult<Order> findBoosterOrders(Long boosterId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findByBooster(boosterId, offset, pageSize);
        long total = orderMapper.countByBooster(boosterId);
        return PageResult.of(list, total, page, pageSize);
    }

    public PageResult<Order> findAllOrders(String status, String game, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Order> list = orderMapper.findAll(status, game, offset, pageSize);
        long total = orderMapper.countAll(status, game);
        return PageResult.of(list, total, page, pageSize);
    }

    public Order assign(Long orderId, Long boosterId, Long csId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待接单状态的订单才能派单");
        }
        orderMapper.assign(orderId, boosterId, csId);
        return orderMapper.findById(orderId);
    }

    public Order createByCs(Long customerId, Long boosterId, Long csId,
                            String game, String detail, BigDecimal price) {
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
