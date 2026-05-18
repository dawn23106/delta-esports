package com.delta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性绑定 — 从 application.yml 的 jwt 节点自动映射。
 * @ConfigurationProperties(prefix = "jwt"):
 *   yml中的 jwt.access-secret → setAccessSecret()
 *   yml中的 jwt.access-ttl    → setAccessTtl()
 *   驼峰自动匹配：access-secret → accessSecret
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String accessSecret;    // AccessToken签名密钥
    private String accessTtl;       // AccessToken有效期（如 15m）
    private String refreshSecret;   // RefreshToken签名密钥
    private String refreshTtl;      // RefreshToken有效期（如 7d）

    public String getAccessSecret() { return accessSecret; }
    public void setAccessSecret(String accessSecret) { this.accessSecret = accessSecret; }
    public String getAccessTtl() { return accessTtl; }
    public void setAccessTtl(String accessTtl) { this.accessTtl = accessTtl; }
    public String getRefreshSecret() { return refreshSecret; }
    public void setRefreshSecret(String refreshSecret) { this.refreshSecret = refreshSecret; }
    public String getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(String refreshTtl) { this.refreshTtl = refreshTtl; }
}
