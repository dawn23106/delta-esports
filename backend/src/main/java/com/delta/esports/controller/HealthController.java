package com.delta.esports.controller;

import com.delta.esports.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:delta-esports}")
    private String applicationName;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<Result<Map<String, Object>>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", applicationName);
        data.put("timestamp", Instant.now().toString());
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            data.put("status", "UP");
            data.put("database", "UP");
            return ResponseEntity.ok(Result.success(data));
        } catch (RuntimeException exception) {
            data.put("status", "DOWN");
            data.put("database", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Result.error(503, "Service unavailable"));
        }
    }
}
