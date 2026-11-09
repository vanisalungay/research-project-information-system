package com.rpis.backend.config;

import com.rpis.backend.util.DatabaseSafetyGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Verifies the database target before the application can serve any request.
 *
 * <p>The shared configuration deliberately has no default JDBC URL, so a missing
 * {@code RPIS_DB_URL} already stops start-up. This class closes the remaining gap: a
 * variable that is present but points somewhere unintended. It compares the database
 * the pool is actually connected to (read from the live connection, never from a
 * property) with {@code rpis.db.expected-name}, and refuses to start on a mismatch.
 *
 * <p>Each profile declares its own expectation: the shared configuration expects the
 * development clone, {@code application-prod.properties} expects the live database and
 * the test/loadtest profiles expect the isolated test database. An empty value disables
 * the check, so nothing breaks for a profile that has not opted in.
 *
 * <p>The guard is read-only: it opens one connection, asks for its URL and closes it.
 */
@Slf4j
@Component
public class DatabaseTargetGuard implements InitializingBean {

    private final DataSource dataSource;
    private final String expectedDatabaseName;

    public DatabaseTargetGuard(DataSource dataSource,
            @Value("${rpis.db.expected-name:}") String expectedDatabaseName) {
        this.dataSource = dataSource;
        this.expectedDatabaseName = expectedDatabaseName;
    }

    @Override
    public void afterPropertiesSet() {
        if (expectedDatabaseName == null || expectedDatabaseName.isBlank()) {
            // Fail closed. The previous behaviour was to log a warning and start anyway,
            // which left the one property that distinguishes the development clone from
            // the live database unverified: a mis-set RPIS_DB_URL would then have started
            // the application against unchecked data with no objection at all. A blank
            // expectation is a configuration error, never an invitation to skip the check.
            throw new IllegalStateException(
                    "REFUSING TO START: rpis.db.expected-name is blank, so the target database cannot be "
                            + "verified. Set RPIS_DB_EXPECTED (for example RPIS_DB_EXPECTED=rpis_db) or "
                            + "remove the blank override so the configured default applies. Nothing was written.");
        }
        String actual = DatabaseSafetyGuard.requireExpectedDatabase(dataSource, expectedDatabaseName,
                "start the application");
        log.info("Database target verified: connected to {}, expected {}.", actual, expectedDatabaseName);
    }
}
