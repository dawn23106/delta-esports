package com.delta.esports.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_ATTEMPTS_PER_MINUTE = 10;

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        String key = ip + ":" + request.getRequestURI();
        if (!rateLimitService.tryAcquire(key, MAX_ATTEMPTS_PER_MINUTE, 60)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        // 应用已开启 server.forward-headers-strategy=framework，
        // getRemoteAddr() 已经根据代理头解析出真实客户端 IP，无需重复读头。
        String ip = request.getRemoteAddr();
        // 防御性处理：若仍包含逗号分隔的多级代理，只取第一个 IP。
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null || ip.isEmpty() ? "unknown" : ip;
    }
}
