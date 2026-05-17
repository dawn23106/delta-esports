package com.delta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String accessSecret;
    private String accessTtl;
    private String refreshSecret;
    private String refreshTtl;

    public String getAccessSecret() { return accessSecret; }
    public void setAccessSecret(String accessSecret) { this.accessSecret = accessSecret; }
    public String getAccessTtl() { return accessTtl; }
    public void setAccessTtl(String accessTtl) { this.accessTtl = accessTtl; }
    public String getRefreshSecret() { return refreshSecret; }
    public void setRefreshSecret(String refreshSecret) { this.refreshSecret = refreshSecret; }
    public String getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(String refreshTtl) { this.refreshTtl = refreshTtl; }
}
