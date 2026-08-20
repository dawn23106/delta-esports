package com.delta.esports.push;

import com.delta.esports.entity.Order;
import com.delta.esports.entity.OrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单领域事件推送。多实例部署时通过 Redis Pub/Sub 做跨实例广播：
 * 发布到 channel 后，所有实例的订阅者（见 RedisPubSubConfig）收到消息并推给本地会话；
 * Redis 不可用或未启用 Pub/Sub 时退化为本地直接推送（单实例语义）。
 */
@Service
public class OrderPushService {

    public static final String CHANNEL = "delta:orders:push";

    private final OrderPushWebSocketHandler handler;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean pubSubEnabled;

    public OrderPushService(OrderPushWebSocketHandler handler, StringRedisTemplate redis,
                            ObjectMapper objectMapper,
                            @Value("${app.redis.pubsub.enabled:false}") boolean pubSubEnabled) {
        this.handler = handler;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.pubSubEnabled = pubSubEnabled;
    }

    /** 推送给指定用户：优先 Redis 广播（跨实例），失败/未启用时本地直推 */
    public void pushToUser(Long userId, String type, Object data) {
        if (userId == null) return;
        if (pubSubEnabled) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("userId", userId);
                payload.put("type", type);
                payload.put("data", data);
                redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(payload));
                return; // 已发布，由订阅端投递给该用户（含本实例）
            } catch (Exception ignored) {
                // Redis 不可用 → 落到本地直推
            }
        }
        handler.pushToUserLocal(userId, type, data);
    }

    /** 订单状态变化时推送给老板与打手 */
    public void pushOrderEvent(Order order) {
        if (order == null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("status", order.getStatus());
        pushToUser(order.getBossId(), "ORDER_EVENT", data);
        pushToUser(order.getBoosterId(), "ORDER_EVENT", data);
    }

    /** 新消息推送给订单双方 */
    public void pushOrderMessage(Long bossId, Long boosterId, OrderMessage message) {
        if (message == null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", message.getOrderId());
        data.put("senderId", message.getSenderId());
        data.put("content", message.getContent());
        data.put("type", message.getType());
        data.put("createdAt", message.getCreatedAt());
        pushToUser(bossId, "ORDER_MESSAGE", data);
        pushToUser(boosterId, "ORDER_MESSAGE", data);
    }
}
