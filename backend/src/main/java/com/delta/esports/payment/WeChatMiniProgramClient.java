package com.delta.esports.payment;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.config.PaymentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Component
public class WeChatMiniProgramClient {
    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public WeChatMiniProgramClient(PaymentProperties properties, ObjectMapper objectMapper,
                                   RestTemplateBuilder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    public String exchangeCodeForOpenId(String loginCode) {
        if (loginCode == null || loginCode.isBlank()) throw new BusinessException(400, "缺少微信登录凭证");
        if (properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            throw new BusinessException(503, "微信小程序 AppSecret 尚未配置");
        }
        String url = UriComponentsBuilder.fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("js_code", loginCode)
                .queryParam("grant_type", "authorization_code")
                .build(true).toUriString();
        try {
            JsonNode result = objectMapper.readTree(restTemplate.getForObject(url, String.class));
            String openId = result.path("openid").asText();
            if (openId.isBlank()) {
                throw new BusinessException(400, result.path("errmsg").asText("微信登录凭证无效"));
            }
            return openId;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(502, "微信登录服务暂时不可用");
        } catch (Exception e) {
            throw new BusinessException(502, "微信登录返回数据格式错误");
        }
    }
}
