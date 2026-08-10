package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.entity.Order;
import com.delta.esports.entity.OrderMessage;
import com.delta.esports.mapper.OrderMapper;
import com.delta.esports.mapper.OrderMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class OrderMessageService {
    private static final Set<String> CHATTABLE_STATUSES = Set.of("assigned", "in_progress");
    private static final Set<String> MESSAGE_TYPES = Set.of("text", "room");

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderMessageMapper messageMapper;

    public List<OrderMessage> list(Long userId, Long orderId) {
        Order order = requireParticipant(userId, orderId);
        if (order.getBoosterId() == null) {
            throw new BusinessException(400, "订单接单后才会开启聊天");
        }
        return messageMapper.selectList(new LambdaQueryWrapper<OrderMessage>()
                .eq(OrderMessage::getOrderId, orderId)
                .orderByAsc(OrderMessage::getCreatedAt));
    }

    public OrderMessage send(Long userId, Long orderId, String content, String type) {
        Order order = requireParticipant(userId, orderId);
        if (!CHATTABLE_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(400, "当前订单状态不能发送消息");
        }
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty() || normalized.length() > 500) {
            throw new BusinessException(400, "消息内容应为 1 到 500 个字符");
        }
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setSenderId(userId);
        message.setContent(normalized);
        String normalizedType = type == null || type.isBlank() ? "text" : type;
        if (!MESSAGE_TYPES.contains(normalizedType)) {
            throw new BusinessException(400, "消息类型不合法");
        }
        message.setType(normalizedType);
        messageMapper.insert(message);
        return message;
    }

    private Order requireParticipant(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        boolean isBoss = userId.equals(order.getBossId());
        boolean isBooster = userId.equals(order.getBoosterId());
        if (!isBoss && !isBooster) throw new BusinessException(403, "无权查看该订单聊天");
        return order;
    }
}
