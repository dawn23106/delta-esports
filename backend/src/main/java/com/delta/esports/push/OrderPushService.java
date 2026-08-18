package com.delta.esports.push;

import com.delta.esports.entity.Order;
import com.delta.esports.entity.OrderMessage;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单领域事件的推送封装。前端未接入时为空操作，不影响现有 REST 轮询。
 */
@Service
public class OrderPushService {

    private final OrderPushWebSocketHandler handler;

    public OrderPushService(OrderPushWebSocketHandler handler) {
        this.handler = handler;
    }

    /** 订单状态变化时推送给老板与打手 */
    public void pushOrderEvent(Order order) {
        if (order == null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("status", order.getStatus());
        if (order.getBossId() != null) {
            handler.pushToUser(order.getBossId(), "ORDER_EVENT", data);
        }
        if (order.getBoosterId() != null) {
            handler.pushToUser(order.getBoosterId(), "ORDER_EVENT", data);
        }
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
        if (bossId != null) handler.pushToUser(bossId, "ORDER_MESSAGE", data);
        if (boosterId != null) handler.pushToUser(boosterId, "ORDER_MESSAGE", data);
    }
}
