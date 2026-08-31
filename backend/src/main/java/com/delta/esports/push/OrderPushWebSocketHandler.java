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
 * 极简订单推送 WebSocket 处理器：按 userId 维护本实例的会话，向本实例连接的客户端推送 JSON 文本。
 * 跨实例广播由 {@link com.delta.esports.push.OrderPushService} 通过 Redis Pub/Sub 完成：
 * 各实例订阅同一 channel，收到消息后调用本类的 {@link #pushToUserLocal} 推给本地会话。
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
        userSessions.entrySet().removeIf(entry -> {
            entry.getValue().remove(session);
            return entry.getValue().isEmpty();
        });
    }

    /** 仅向本实例上的该用户会话推送 */
    public void pushToUserLocal(Long userId, String type, Object data) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", type);
        message.put("data", data);
        try {
            String json = objectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                try {
                    // Spring WebSocketSession 不保证并发 sendMessage 安全；会话只属于
                    // 当前 JVM，因此以 session 为粒度串行发送，不阻塞其他用户的会话。
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(json));
                        } else {
                            sessions.remove(session);
                        }
                    }
                } catch (Exception sendFailure) {
                    sessions.remove(session); // 失效会话及时清理，不影响其它会话
                }
            }
            if (sessions.isEmpty()) {
                userSessions.remove(userId, sessions);
            }
        } catch (Exception ignored) {
            // 序列化失败忽略
        }
    }
}
