package com.rpis.backend.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-backed tests for {@link PostgresAdvisoryLock}.
 *
 * <p>Every test uses the real PostgreSQL server, because the property under test is a
 * property of the database: two different sessions must not both hold the same lock.
 * A mock could not demonstrate that, and neither could a JVM-level lock, which is
 * precisely the thing this class exists to replace.
 *
 * <p>Each test takes two connections from the pool, so the pool must have at least two.
 * The test profile inherits the shared maximum of 40.
 *
 * <p>Deliberately <strong>not</strong> {@code @Transactional}: the lock is held on its
 * own connection for the life of the object, and wrapping the test in a transaction
 * would have the two acquisitions share one connection, which would make the lock
 * re-entrant and the contention test meaningless.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostgresAdvisoryLockTest {

    private static final String PURPOSE_NAME = "the advisory-lock test";

    @Autowired
    private DataSource dataSource;

    private Optional<PostgresAdvisoryLock> acquire(int purpose) {
        return PostgresAdvisoryLock.tryAcquire(dataSource, purpose, PURPOSE_NAME);
    }

    @Test
    void acquiresTheLockWhenNothingElseHoldsIt() {
        try (PostgresAdvisoryLock lock =
                     acquire(PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED).orElseThrow()) {
            assertTrue(lock.isHeld(), "a freshly acquired lock must report that it is held");
        }
    }

    @Test
    void aSecondInstanceIsRefusedWhileTheFirstStillHoldsTheLock() {
        try (PostgresAdvisoryLock first =
                     acquire(PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED).orElseThrow()) {
            assertTrue(first.isHeld());

            Optional<PostgresAdvisoryLock> second =
                    acquire(PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED);

            assertTrue(second.isEmpty(),
                    "a second holder must be refused, otherwise two instances would seed or "
                            + "migrate at the same time");
        }
    }

    @Test
    void theLockIsAvailableAgainOnceItHasBeenReleased() {
        PostgresAdvisoryLock first = acquire(PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED).orElseThrow();
        first.close();
        assertFalse(first.isHeld(), "a released lock must not still report itself as held");

        try (PostgresAdvisoryLock second =
                     acquire(PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED).orElseThrow()) {
            assertTrue(second.isHeld(),
                    "the lock must be free again after release, or a restart could never take it");
        }
    }

    @Test
    void closingTwiceIsHarmless() {
        PostgresAdvisoryLock lock = acquire(PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED).orElseThrow();
        lock.close();
        lock.close();
        assertFalse(lock.isHeld());
    }

    @Test
    void theTwoPurposesDoNotBlockEachOther() {
        try (PostgresAdvisoryLock migration =
                     acquire(PostgresAdvisoryLock.PURPOSE_PASSWORD_MIGRATION).orElseThrow();
             PostgresAdvisoryLock seed =
                     acquire(PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED).orElseThrow()) {

            assertTrue(migration.isHeld(), "the migration lock must be independent of the seed lock");
            assertTrue(seed.isHeld(), "the seed lock must be independent of the migration lock");
        }
    }

    @Test
    void anUnreachableDatabaseIsReportedRatherThanSilentlyIgnored() {
        DataSource unreachable = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:1/rpis_test?connectTimeout=2&socketTimeout=2",
                "postgres", "not-used");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> PostgresAdvisoryLock.tryAcquire(unreachable, 99, PURPOSE_NAME),
                "a lock that cannot even be requested must fail loudly, so the caller knows "
                        + "not to run the guarded step");

        assertTrue(failure.getMessage().contains(PURPOSE_NAME),
                "the failure must name the step it was protecting, but was: " + failure.getMessage());
    }
}