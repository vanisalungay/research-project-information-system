package com.rpis.backend.controller;

import com.rpis.backend.util.DatabaseSafetyGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liveness and readiness probes.
 *
 * <p>A load balancer needs two different questions answered:
 * <ul>
 *   <li><em>is this process alive?</em> - {@code /api/health}, no dependencies
 *       touched, so a momentarily slow database does not get every instance
 *       removed from the pool at once;</li>
 *   <li><em>can this instance actually serve traffic?</em> - {@code /api/health/ready},
 *       which checks the database connection it depends on and answers 503 when it
 *       cannot reach it, so the balancer stops sending requests to a broken
 *       instance instead of forwarding them into a 500.</li>
 * </ul>
 *
 * <p>The previous {@code /api/test-token} endpoint has been removed. It echoed the
 * user id and role decoded from any token it was handed, which is a debugging
 * aid that must not exist on a public deployment; the same check is available in
 * the test suite.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    /** Liveness: the process is up. Deliberately touches no external dependency. */
    @GetMapping("/health")
    public String healthCheck() {
        return "RPIS backend is running";
    }

    /**
     * Readiness: the instance can serve requests, meaning it can reach its
     * database. 503 tells the load balancer to take this instance out of rotation.
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            body.put("status", "UP");
            body.put("database", DatabaseSafetyGuard.describe(dataSource));
            body.put("databaseReachable", one != null && one == 1);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            // Log the cause once per probe so an operator can see why the instance
            // was removed from rotation.
            log.warn("Readiness probe failed: {}", e.getMessage());
            body.put("status", "DOWN");
            body.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}

