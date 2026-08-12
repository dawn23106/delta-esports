package com.delta.esports.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private boolean enabled;
    private boolean mockEnabled;
    private String provider = "yungouos";
    private String apiBaseUrl = "https://api.pay.yungouos.com";
    private String merchantId;
    private String apiKey;
    private String appId;
    private String appSecret;
    private String payNotifyUrl;
    private String refundNotifyUrl;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
}
