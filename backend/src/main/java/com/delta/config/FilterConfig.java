package com.delta.config;

import com.delta.security.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器注册配置 — 将 JwtAuthFilter 注册为 Servlet Filter。
 * Filter 比 Interceptor 更早执行，在请求到达 Controller 前就完成认证。
 *
 * addUrlPatterns("/api/*") — 只拦截 /api/ 开头的请求
 * setOrder(1)             — 执行顺序，数字越小越先执行
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/api/*");
        reg.setOrder(1);
        return reg;
    }
}
