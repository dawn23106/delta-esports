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

@Component
public class JwtAuthFilter extends HttpFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    private static final List<String> WHITELIST = List.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/refresh"
    );

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String path = req.getRequestURI();
        for (String w : WHITELIST) {
            if (path.equals(w)) { chain.doFilter(req, res); return; }
        }
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            res.setStatus(401);
            res.setContentType("application/json;charset=utf-8");
            res.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return;
        }
        try {
            Claims claims = jwtUtil.parseAccessToken(header.substring(7));
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
