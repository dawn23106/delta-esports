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

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(Long userId, String role) {
        return buildToken(userId, role, jwtProperties.getAccessSecret(), jwtProperties.getAccessTtl());
    }

    public String generateRefreshToken(Long userId, String role) {
        return buildToken(userId, role, jwtProperties.getRefreshSecret(), jwtProperties.getRefreshTtl());
    }

    public Claims parseAccessToken(String token) {
        return parseToken(token, jwtProperties.getAccessSecret());
    }

    public Claims parseRefreshToken(String token) {
        return parseToken(token, jwtProperties.getRefreshSecret());
    }

    private String buildToken(Long userId, String role, String secret, String ttl) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long ttlMs = parseTtl(ttl);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }

    private Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

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
