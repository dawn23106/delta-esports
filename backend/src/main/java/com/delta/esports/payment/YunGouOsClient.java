package com.delta.esports.payment;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.config.PaymentProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class YunGouOsClient implements PaymentGateway {
    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public YunGouOsClient(PaymentProperties properties, ObjectMapper objectMapper,
                          RestTemplateBuilder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    @Override
    public String providerCode() {
        return "yungouos";
    }

    @Override
    public String merchantId() {
        return properties.getMerchantId();
    }

    @Override
    public Map<String, Object> prepareMiniProgramPayment(String outTradeNo, BigDecimal amount,
                                                         String body, String openId) {
        ensureConfigured();
        Map<String, Object> signed = new LinkedHashMap<>();
        signed.put("out_trade_no", outTradeNo);
        signed.put("total_fee", money(amount));
        signed.put("mch_id", properties.getMerchantId());
        signed.put("body", body);
        signed.put("app_id", properties.getAppId());
        signed.put("open_id", openId);

        Map<String, Object> params = new LinkedHashMap<>(signed);
        params.put("attach", outTradeNo);
        params.put("notify_url", properties.getPayNotifyUrl());
        params.put("sign", YunGouOsSigner.sign(signed, properties.getApiKey()));
        return postForData("/api/pay/wxpay/v3/minAppPay", params);
    }

    @Override
    public Map<String, Object> query(String outTradeNo) {
        ensureConfigured();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("out_trade_no", outTradeNo);
        params.put("mch_id", properties.getMerchantId());
        params.put("sign", YunGouOsSigner.sign(params, properties.getApiKey()));
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl() + "/api/system/order/getPayOrderInfo")
                .queryParam("out_trade_no", outTradeNo)
                .queryParam("mch_id", properties.getMerchantId())
                .queryParam("sign", params.get("sign"))
                .build(true).toUriString();
        return getForData(url);
    }

    @Override
    public Map<String, Object> refund(String outTradeNo, BigDecimal amount, String refundNo) {
        ensureConfigured();
        if (blank(properties.getRefundNotifyUrl()) || !properties.getRefundNotifyUrl().startsWith("https://")) {
            throw new BusinessException(503, "退款回调必须配置公网 HTTPS 地址");
        }
        Map<String, Object> signed = new LinkedHashMap<>();
        signed.put("out_trade_no", outTradeNo);
        signed.put("mch_id", properties.getMerchantId());
        signed.put("money", money(amount));
        Map<String, Object> params = new LinkedHashMap<>(signed);
        params.put("out_trade_refund_no", refundNo);
        params.put("refund_desc", "用户取消订单");
        if (properties.getRefundNotifyUrl() != null && !properties.getRefundNotifyUrl().isBlank()) {
            params.put("notify_url", properties.getRefundNotifyUrl());
        }
        params.put("sign", YunGouOsSigner.sign(signed, properties.getApiKey()));
        return postForData("/api/pay/wxpay/refundOrder", params);
    }

    @Override
    public void close(String outTradeNo) {
        ensureConfigured();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("mch_id", properties.getMerchantId());
        params.put("out_trade_no", outTradeNo);
        params.put("sign", YunGouOsSigner.sign(params, properties.getApiKey()));
        postForData("/api/pay/wxpay/closeOrder", params);
    }

    @Override
    public boolean verifyPayCallback(Map<String, String> callback) {
        Map<String, Object> signed = new LinkedHashMap<>();
        signed.put("code", callback.get("code"));
        signed.put("orderNo", callback.get("orderNo"));
        signed.put("outTradeNo", callback.get("outTradeNo"));
        signed.put("payNo", callback.get("payNo"));
        signed.put("money", callback.get("money"));
        signed.put("mchId", callback.get("mchId"));
        return YunGouOsSigner.verify(callback, signed, properties.getApiKey());
    }

    @Override
    public boolean verifyRefundCallback(Map<String, String> callback) {
        Map<String, Object> signed = new LinkedHashMap<>();
        String[] fields = {"code", "refundNo", "orderNo", "outTradeNo", "payNo", "mchId",
                "payName", "refundMoney", "channel", "refundTime", "payRefundNo", "applyTime"};
        for (String field : fields) signed.put(field, callback.get(field));
        return YunGouOsSigner.verify(callback, signed, properties.getApiKey());
    }

    private Map<String, Object> postForData(String path, Map<String, Object> params) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach((key, value) -> {
            if (value != null && !String.valueOf(value).isBlank()) form.add(key, String.valueOf(value));
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            String body = restTemplate.postForObject(baseUrl() + path, new HttpEntity<>(form, headers), String.class);
            return readData(body);
        } catch (RestClientException e) {
            throw new BusinessException(502, "微信支付通道暂时不可用");
        }
    }

    private Map<String, Object> getForData(String url) {
        try {
            return readData(restTemplate.getForObject(url, String.class));
        } catch (RestClientException e) {
            throw new BusinessException(502, "支付结果查询暂时不可用");
        }
    }

    private Map<String, Object> readData(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("code").asInt(-1) != 0) {
                throw new BusinessException(502, root.path("msg").asText("支付通道请求失败"));
            }
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) throw new BusinessException(502, "支付通道返回数据为空");
            if (!data.isObject()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("value", objectMapper.convertValue(data, Object.class));
                return result;
            }
            return objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(502, "支付通道返回数据格式错误");
        }
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) throw new BusinessException(503, "在线支付尚未启用");
        if (blank(properties.getMerchantId()) || blank(properties.getApiKey()) || blank(properties.getAppId())) {
            throw new BusinessException(503, "微信支付商户配置不完整");
        }
        if (blank(properties.getPayNotifyUrl()) || !properties.getPayNotifyUrl().startsWith("https://")) {
            throw new BusinessException(503, "支付回调必须配置公网 HTTPS 地址");
        }
    }

    private String baseUrl() {
        return properties.getApiBaseUrl().replaceAll("/+$", "");
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
