package com.delta.esports.push;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 从握手 URL 的 query 中解析 userId，供处理器按用户定向推送。
 */
public class UserIdHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                if (pair.startsWith("userId=")) {
                    try {
                        attributes.put("userId", Long.valueOf(pair.substring("userId=".length())));
                    } catch (NumberFormatException ignored) {
                        // 非法 userId 不注册，避免类型异常
                    }
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
