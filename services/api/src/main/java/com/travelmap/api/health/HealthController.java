package com.travelmap.api.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final JdbcTemplate jdbc;
    public HealthController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping
    Map<String, Object> health() {
        Integer database = jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("status", database != null && database == 1 ? "UP" : "DOWN", "timestamp", Instant.now());
    }
}
