package com.delta.esports.config;

import com.delta.esports.push.OrderPushService;
import com.delta.esports.push.OrderPushWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 订阅配置：各实例订阅订单推送 channel，
 * 收到跨实例广播后由本实例的 {@link OrderPushWebSocketHandler} 投递给本地连接的客户端。
 * 仅在 {@code app.redis.pubsub.enabled=true}（生产默认）时创建容器；
 * 本地开发/测试无 Redis 时不创建，避免无意义的连接重试。
 */
@Configuration
@ConditionalOnProperty(name = "app.redis.pubsub.enabled", havingValue = "true")
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            OrderPushWebSocketHandler pushHandler,
            ObjectMapper objectMapper) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                JsonNode root = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
                long userId = root.path("userId").asLong(0);
                String type = root.path("type").asText();
                if (userId > 0) {
                    pushHandler.pushToUserLocal(userId, type, root.path("data"));
                }
            } catch (Exception ignored) {
                // 单条消息解析/投递失败不影响其它消息
            }
        }, new ChannelTopic(OrderPushService.CHANNEL));
        return container;
    }
}
