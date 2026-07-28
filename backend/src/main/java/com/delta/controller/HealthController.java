package com.delta.controller;

import com.delta.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of("status", "UP", "time", Instant.now().toString()));
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return """
                <!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>游戏陪练订单撮合平台</title>
                <style>body{font-family:system-ui;margin:0;background:#f5f7fb;color:#172033}.wrap{max-width:760px;margin:10vh auto;padding:40px;background:white;border-radius:18px;box-shadow:0 12px 40px #1f29371f}a{display:inline-block;margin:10px 12px 0 0;padding:12px 18px;border-radius:10px;background:#2563eb;color:white;text-decoration:none}.muted{color:#64748b}code{background:#eef2ff;padding:3px 7px;border-radius:6px}</style>
                </head><body><main class="wrap"><h1>游戏陪练订单撮合平台</h1>
                <p>Spring Boot + MyBatis + JWT + Vue 3 的多角色订单状态流转演示。</p>
                <p><a href="/app/">玩家 / 服务者端</a><a href="/admin/">客服管理端</a></p>
                <p class="muted">演示客服：<code>13800000000</code> / <code>cs123456</code></p>
                <p class="muted">健康检查：<a href="/api/health" style="padding:0;background:none;color:#2563eb">/api/health</a></p>
                </main></body></html>
                """;
    }
}
