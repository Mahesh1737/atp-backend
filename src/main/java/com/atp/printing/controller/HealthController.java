package com.atp.printing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("application", appName);
        health.put("timestamp", LocalDateTime.now());
        health.put("baseUrl", baseUrl);

        // Check database connection
        try (Connection connection = dataSource.getConnection()) {
            health.put("database", "Connected");
            health.put("databaseUrl", connection.getMetaData().getURL());
        } catch (Exception e) {
            health.put("database", "Disconnected: " + e.getMessage());
            health.put("status", "DOWN");
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "ATP Printing Service API");
        response.put("version", "1.0.0");
        response.put("status", "Running");
        response.put("documentation", baseUrl + "/api-docs");
        return ResponseEntity.ok(response);
    }
}