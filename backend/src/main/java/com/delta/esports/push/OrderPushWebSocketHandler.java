package com.delta.esports.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 极简订单推送 WebSocket 处理器：按 userId 维护会话，向指定用户推送 JSON 文本。
 * 握手地址：/ws/orders?userId={userId}
 */
@Component
public class OrderPushWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object uid = session.getAttributes().get("userId");
        if (uid instanceof Long) {
            userSessions.computeIfAbsent((Long) uid, k -> ConcurrentHashMap.newKeySet()).add(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.values().forEach(set -> set.remove(session));
    }

    public void pushToUser(Long userId, String type, Object data) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", type);
        message.put("data", data);
        try {
            String json = objectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (Exception ignored) {
                    // 单个会话发送失败不影响其它会话
                }
            }
        } catch (Exception ignored) {
            // 序列化失败忽略
        }
    }
}
