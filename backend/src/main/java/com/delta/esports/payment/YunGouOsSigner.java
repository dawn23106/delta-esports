package com.delta.esports.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class YunGouOsSigner {
    private YunGouOsSigner() {}

    public static String sign(Map<String, ?> source, String payKey) {
        if (payKey == null || payKey.isBlank()) {
            throw new IllegalArgumentException("支付通道密钥未配置");
        }
        String body = new TreeMap<>(source).entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null && !String.valueOf(entry.getValue()).isBlank())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return md5(body + "&key=" + payKey).toUpperCase();
    }

    public static boolean verify(Map<String, String> source, Map<String, Object> signedFields, String payKey) {
        String received = source.get("sign");
        return received != null && MessageDigest.isEqual(
                received.toUpperCase().getBytes(StandardCharsets.UTF_8),
                sign(signedFields, payKey).getBytes(StandardCharsets.UTF_8));
    }

    private static String md5(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) result.append(String.format("%02x", b & 0xff));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("无法生成支付签名", e);
        }
    }
}
