package com.delta.esports.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements ApplicationRunner {
    private final PaymentProperties payment;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public ProductionConfigurationValidator(PaymentProperties payment) {
        this.payment = payment;
    }

    @Override
    public void run(ApplicationArguments args) {
        require(!blank(allowedOrigins), "CORS_ORIGINS must be configured in production");
        require(!allowedOrigins.contains("*"), "CORS_ORIGINS must not contain a wildcard in production");
        require(!allowedOrigins.contains("localhost") && !allowedOrigins.contains("127.0.0.1"),
                "CORS_ORIGINS must use public production origins");

        if (!payment.isEnabled()) return;
        require(!payment.isMockEnabled(), "Mock payment must be disabled in production");
        require(!blank(payment.getMerchantId()), "PAYMENT_MERCHANT_ID is required when payment is enabled");
        require(!blank(payment.getApiKey()), "PAYMENT_API_KEY is required when payment is enabled");
        require(!blank(payment.getAppId()), "WECHAT_MINIPROGRAM_APP_ID is required when payment is enabled");
        require(!blank(payment.getAppSecret()), "WECHAT_MINIPROGRAM_APP_SECRET is required when payment is enabled");
        require(https(payment.getPayNotifyUrl()), "PAYMENT_PAY_NOTIFY_URL must be a public HTTPS URL");
        require(https(payment.getRefundNotifyUrl()), "PAYMENT_REFUND_NOTIFY_URL must be a public HTTPS URL");
    }

    private boolean https(String value) {
        if (blank(value)) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && !blank(uri.getHost());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
