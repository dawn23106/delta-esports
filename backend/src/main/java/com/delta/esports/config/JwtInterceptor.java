package com.delta.esports.config;

import com.delta.esports.common.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        boolean publicRead = "GET".equalsIgnoreCase(request.getMethod())
                && (requestUri.equals("/api/services")
                || requestUri.matches("/api/services/\\d+")
                || requestUri.equals("/api/users/boosters")
                || requestUri.equals("/api/announcements"));
        if (publicRead) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            Map<String, Object> body = new HashMap<>();
            body.put("code", 401);
            body.put("message", "未登录");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        String token = authHeader.substring(7);
        // 只解析一次：签名、过期、tokenType 一起校验，避免重复 parseToken
        Claims claims = jwtUtils.parseValidAccessToken(token);
        if (claims == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            Map<String, Object> body = new HashMap<>();
            body.put("code", 401);
            body.put("message", "token已过期，请重新登录");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        request.setAttribute("userId", Long.valueOf(claims.getSubject()));
        request.setAttribute("role", claims.get("role", String.class));
        return true;
    }
}
