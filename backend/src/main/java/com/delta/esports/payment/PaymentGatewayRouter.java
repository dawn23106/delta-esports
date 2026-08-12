package com.delta.esports.payment;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.config.PaymentProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentGatewayRouter {
    private final List<PaymentGateway> gateways;
    private final PaymentProperties properties;

    public PaymentGatewayRouter(List<PaymentGateway> gateways, PaymentProperties properties) {
        this.gateways = gateways;
        this.properties = properties;
    }

    public PaymentGateway active() {
        return gateways.stream()
                .filter(gateway -> gateway.providerCode().equalsIgnoreCase(properties.getProvider()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(503, "当前配置的支付通道不可用"));
    }
}
