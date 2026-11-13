package com.rpis.backend.controller;

import com.rpis.backend.service.UserService;
import com.rpis.backend.util.DatabaseSafetyGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Starts the (optional) synthetic data steps from a request.
 *
 * <p>This endpoint used to live on {@code UserController} as a public, unconditional
 * route, which let anyone create the seven demonstration accounts - with published
 * passwords - in whatever database the application was pointed at. It now requires
 * an explicit opt-in ({@code rpis.seed.default-users.enabled}), the {@code RPS_ADMIN}
 * role is enforced in {@code SecurityConfig}, and the target database must be the
 * isolated test database, so a misconfiguration cannot write into production.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DevSeedController {

    private final UserService userService;
    private final DataSource dataSource;

    @Value("${rpis.seed.default-users.enabled:false}")
    private boolean defaultUsersSeedingEnabled;

    @PostMapping("/users/seed")
    public ResponseEntity<?> seedUsers() {
        if (!defaultUsersSeedingEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Default demo account seeding is disabled."));
        }
        // Reads the live DataSource, not a configuration property, and fails with
        // 403 without writing anything unless the target is rpis_test.
        DatabaseSafetyGuard.requireTestDatabase(dataSource, "seed the default demo accounts");

        userService.seedUsers();
        log.warn("Default demo accounts were seeded through the API (target database: {}).",
                DatabaseSafetyGuard.describe(dataSource));
        return ResponseEntity.ok("Default demo accounts are in place.");
    }
}
