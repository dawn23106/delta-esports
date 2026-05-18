package com.delta.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 — 拦截所有 /api/* 请求（白名单除外），校验 AccessToken。
 *
 * 流程：
 *   1. 请求路径在 WHITELIST 中 → 直接放行（登录/注册/刷新不需要token）
 *   2. 从 Authorization header 取 Bearer token
 *   3. 解析 JWT，提取 userId 和 role 注入到 request.setAttribute
 *   4. 后续 Controller 从 request.getAttribute("userId") 取当前用户
 *
 * 继承 HttpFilter 而非 OncePerRequestFilter：因为注册为 FilterRegistrationBean 后
 * 已保证了过滤规则，HttpFilter 更轻量。
 */
@Component
public class JwtAuthFilter extends HttpFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** 白名单 — 这些接口不需要Token */
    private static final List<String> WHITELIST = List.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/refresh"
    );

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String path = req.getRequestURI();

        // 1. 白名单放行
        for (String w : WHITELIST) {
            if (path.equals(w)) { chain.doFilter(req, res); return; }
        }

        // 2. 提取 Token
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            res.setStatus(401);
            res.setContentType("application/json;charset=utf-8");
            res.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return;
        }

        // 3. 解析Token并注入用户信息
        try {
            Claims claims = jwtUtil.parseAccessToken(header.substring(7));
            // 注入到request，后续Controller通过 request.getAttribute("userId") 获取
            req.setAttribute("userId", Long.parseLong(claims.getSubject()));
            req.setAttribute("role", claims.get("role"));
            chain.doFilter(req, res);
        } catch (ExpiredJwtException e) {
            res.setStatus(401);
            res.setContentType("application/json;charset=utf-8");
            res.getWriter().write("{\"code\":401,\"msg\":\"token已过期\"}");
        } catch (Exception e) {
            res.setStatus(401);
            res.setContentType("application/json;charset=utf-8");
            res.getWriter().write("{\"code\":401,\"msg\":\"token无效\"}");
        }
    }
}
