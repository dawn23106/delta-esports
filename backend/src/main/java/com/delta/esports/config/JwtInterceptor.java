package com.delta.esports.config;

import com.delta.esports.common.JwtUtils;
import com.delta.esports.entity.User;
import com.delta.esports.mapper.UserMapper;
import com.delta.esports.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final long USER_STATUS_TTL_MINUTES = 5;

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final RedisCacheTemplate cache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtInterceptor(JwtUtils jwtUtils, UserMapper userMapper, RedisCacheTemplate cache) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.cache = cache;
    }

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
            return reject(response, 401, "未登录");
        }

        String token = authHeader.substring(7);
        // 只解析一次：签名、过期、tokenType 一起校验，避免重复 parseToken
        Claims claims = jwtUtils.parseValidAccessToken(token);
        if (claims == null) {
            return reject(response, 401, "token已过期，请重新登录");
        }

        Long userId = Long.valueOf(claims.getSubject());
        // 用户状态走 Redis 缓存（TTL 5 分钟），封禁/解封时由 updateStatus 主动失效，保证立即生效
        String status = loadUserStatus(userId);
        if (status == null) {
            return reject(response, 401, "账号不存在");
        }
        if ("banned".equals(status)) {
            return reject(response, 403, "账号已被禁用");
        }

        request.setAttribute("userId", userId);
        request.setAttribute("role", claims.get("role", String.class));
        return true;
    }

    private String loadUserStatus(Long userId) {
        String key = UserService.statusCacheKey(userId);
        String status = cache.get(key, String.class);
        if (status != null) {
            return status;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        cache.set(key, user.getStatus(), USER_STATUS_TTL_MINUTES, TimeUnit.MINUTES);
        return user.getStatus();
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
