package com.delta.esports.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 第三方支付通道的稳定边界。订单业务只依赖本接口，不感知具体服务商。
 */
public interface PaymentGateway {
    String providerCode();
    String merchantId();
    Map<String, Object> prepareMiniProgramPayment(String outTradeNo, BigDecimal amount,
                                                  String body, String openId);
    Map<String, Object> query(String outTradeNo);
    Map<String, Object> refund(String outTradeNo, BigDecimal amount, String refundNo);
    void close(String outTradeNo);
    boolean verifyPayCallback(Map<String, String> callback);
    boolean verifyRefundCallback(Map<String, String> callback);
}
