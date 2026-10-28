package com.rpis.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.rpis.backend.service.UserService;
import com.rpis.backend.util.DatabaseSafetyGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@Slf4j
@SpringBootApplication
public class RpisBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RpisBackendApplication.class, args);
	}

	/**
	 * Optionally creates the demonstration accounts at start-up.
	 *
	 * <p>This used to run unconditionally on every start-up of every instance, which
	 * meant a load-balanced deployment wrote the same seven known-password accounts
	 * into the shared database on every boot (and rewrote every existing account
	 * while doing it). It is now off unless {@code rpis.seed.default-users.enabled}
	 * is explicitly enabled, and a failure here is logged and never prevents the
	 * application from starting.
	 */
	@Bean
	public CommandLineRunner commandLineRunner(
			UserService userService,
			DataSource dataSource,
			@Value("${rpis.seed.default-users.enabled:false}") boolean defaultUsersSeedingEnabled) {
		return args -> {
			if (!defaultUsersSeedingEnabled) {
				log.info("Default demo account seeding is disabled; skipping (target database: {}).",
						DatabaseSafetyGuard.describe(dataSource));
				return;
			}
			log.warn("Default demo account seeding is ENABLED (target database: {}). "
					+ "These accounts have published passwords and must never be used in production.",
					DatabaseSafetyGuard.describe(dataSource));
			try {
				userService.seedUsers();
				log.info("Default demo accounts are in place.");
			} catch (Exception e) {
				log.warn("Could not seed the default demo accounts: {}", e.getMessage());
			}
		};
	}
}

