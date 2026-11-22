package com.rpis.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.rpis.backend.service.UserService;
import com.rpis.backend.util.DatabaseSafetyGuard;
import com.rpis.backend.util.RpisTimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@Slf4j
@SpringBootApplication
public class RpisBackendApplication {

	public static void main(String[] args) {
		// Pin the institution's zone before Hibernate, Jackson or any LocalDateTime.now()
		// call can read the host's default. The notification serializer uses the same zone
		// to emit an explicit offset in the API response.
		String configuredZone = System.getenv().getOrDefault("RPIS_TIME_ZONE", "Asia/Manila");
		try {
			RpisTimeZone.use(configuredZone);
		} catch (RuntimeException invalidZone) {
			log.warn("Ignoring invalid RPIS_TIME_ZONE '{}'; keeping the JVM default ({}).",
					configuredZone, RpisTimeZone.zone());
		}
		// The Hikari connection-init SQL interpolates this property. Set the resolved
		// (validated) zone so a malformed environment value can never reach SQL and the
		// Java clock and the database session always agree.
		System.setProperty("rpis.time-zone", RpisTimeZone.zone().getId());
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

