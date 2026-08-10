package com.delta.esports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PreparePaymentResponse {
    private Long orderId;
    private String outTradeNo;
    private String status;
    private boolean mock;
    private Map<String, Object> paymentParams;
}
