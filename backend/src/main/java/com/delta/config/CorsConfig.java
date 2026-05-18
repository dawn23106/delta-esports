package com.delta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置 — 前后端分离时，前端(localhost:5173)请求后端(localhost:8080)是跨域请求。
 * 浏览器会自动发 OPTIONS 预检请求，后端需要返回允许跨域的响应头。
 *
 * setAllowedOriginPatterns(*)  — 允许任意来源（开发环境用，生产需改为具体域名）
 * setAllowCredentials(true)     — 允许携带 Cookie
 * registerCorsConfiguration(/**): 对所有路径生效
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
