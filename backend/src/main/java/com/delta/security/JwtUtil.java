package com.delta.security;

import com.delta.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 — 生成和解析 Token。
 * 使用双密钥：AccessToken 和 RefreshToken 用不同的密钥签名，互相无法伪造。
 *
 * JWT 结构：Header.Payload.Signature
 *   Header:  算法 + Token类型
 *   Payload: 自定义数据（userId, role）+ 签发时间 + 过期时间
 *   Signature: 用密钥对前两部分签名，防篡改
 */
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /** 生成AccessToken — 15分钟有效期，用于接口调用 */
    public String generateAccessToken(Long userId, String role) {
        return buildToken(userId, role, jwtProperties.getAccessSecret(), jwtProperties.getAccessTtl());
    }

    /** 生成RefreshToken — 7天有效期，用于续期AccessToken */
    public String generateRefreshToken(Long userId, String role) {
        return buildToken(userId, role, jwtProperties.getRefreshSecret(), jwtProperties.getRefreshTtl());
    }

    /** 解析AccessToken — 失败抛异常 */
    public Claims parseAccessToken(String token) {
        return parseToken(token, jwtProperties.getAccessSecret());
    }

    /** 解析RefreshToken — 失败抛异常 */
    public Claims parseRefreshToken(String token) {
        return parseToken(token, jwtProperties.getRefreshSecret());
    }

    /** 构建JWT — subject存userId，claims存role */
    private String buildToken(Long userId, String role, String secret, String ttl) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long ttlMs = parseTtl(ttl);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);       // 自定义字段，JWT过滤器中取出做权限校验
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)          // HMAC-SHA256签名
                .compact();
    }

    /** 解析JWT — 验签 + 解码payload */
    private Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** 解析TTL配置 — 支持 15m / 7d / 1h / 30s 等格式 */
    private long parseTtl(String ttl) {
        ttl = ttl.trim();
        char unit = ttl.charAt(ttl.length() - 1);
        long num = Long.parseLong(ttl.substring(0, ttl.length() - 1));
        return switch (unit) {
            case 's' -> num * 1000L;
            case 'm' -> num * 60 * 1000L;
            case 'h' -> num * 3600 * 1000L;
            case 'd' -> num * 86400 * 1000L;
            default -> num;
        };
    }
}
