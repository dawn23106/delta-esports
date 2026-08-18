package com.delta.esports.config;

import com.delta.esports.push.OrderPushWebSocketHandler;
import com.delta.esports.push.UserIdHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderPushWebSocketHandler orderPushHandler;

    public WebSocketConfig(OrderPushWebSocketHandler orderPushHandler) {
        this.orderPushHandler = orderPushHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderPushHandler, "/ws/orders")
                .addInterceptors(new UserIdHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
