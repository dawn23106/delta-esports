package com.delta.esports.config;

import com.delta.esports.common.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        if (jwtUtils.isTokenExpired(token) || !jwtUtils.isAccessToken(token)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            Map<String, Object> body = new HashMap<>();
            body.put("code", 401);
            body.put("message", "token已过期，请重新登录");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        String role = jwtUtils.getRoleFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);
        return true;
    }
}
