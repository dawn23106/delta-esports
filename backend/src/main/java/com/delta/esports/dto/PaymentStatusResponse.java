package com.delta.esports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentStatusResponse {
    private Long orderId;
    private String orderStatus;
    private String paymentStatus;
    private String outTradeNo;
    private BigDecimal amount;
}
