package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that the database-side plain-text count agrees with the Java definition it
 * replaced.
 *
 * <p>{@code UserRepository.countPlainTextPasswords()} states the "is this already a
 * BCrypt hash?" rule in SQL, while {@code PasswordMigrationService} applies the same rule
 * in Java through {@link PasswordService#isHashed}. Two statements of one rule will drift
 * apart unless something checks them against each other, and if they drift the operator
 * verification step in the deployment runbook becomes a lie: it could report zero
 * plain-text passwords while the migration still has rows to convert, or the reverse.
 *
 * <p>Assertions are written as deltas rather than absolute counts. The test database is
 * shared with the rest of the suite, so what is already in the table is not this test
 * business - only how the count reacts to the rows this test adds.
 *
 * <p>Runs against the isolated {@code rpis_test} database only, and every test is
 * transactional so the rows it creates are rolled back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordPlainTextCountTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** 53 characters of salt and digest: a hash under both definitions. */
    private static final String VALID_HASH = "$2a$10$" + "A".repeat(53);

    /** One character short, so it is a hash under neither definition. */
    private static final String TRUNCATED_HASH = "$2a$10$" + "A".repeat(52);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private DataSource dataSource;

    /** Safety guard: these tests must never run against a real database. */
    private void requireTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "The plain-text count test must run against the isolated rpis_test database, "
                            + "but connected to: " + url);
        }
    }

    private long countInDatabase() {
        userRepository.flush();
        return userRepository.countPlainTextPasswords();
    }

    /** The same rule as the query, but applied in Java - the definition being compared. */
    private long countInJava() {
        userRepository.flush();
        return userRepository.findAll().stream()
                .filter(user -> {
                    String stored = user.getPassword();
                    return stored != null && !stored.isBlank() && !passwordService.isHashed(stored);
                })
                .count();
    }

    private void insertUserWithPassword(String password) {
        User user = new User();
        user.setName("Plain Text Count Test");
        user.setEmail("plaintext.count." + SEQUENCE.incrementAndGet() + "@msunaawan.edu.ph");
        user.setPassword(password);
        user.setRole("PROPONENT");
        user.setStatus("PENDING");
        userRepository.saveAndFlush(user);
    }

    @Test
    void aPlainTextPasswordIsCountedAndAHashIsNot() throws Exception {
        requireTestDatabase();

        String[] plainText = {
                "plain",
                "password1234",
                "LoadTest#2026",
                TRUNCATED_HASH,
                "$2a$10$" + "A".repeat(54),
                "$3a$10$" + "A".repeat(53),
        };
        String[] alreadyHashed = {
                VALID_HASH,
                "$2b$12$" + "A".repeat(53),
                "$2y$10$" + "A".repeat(53),
        };

        for (String value : plainText) {
            assertFalse(passwordService.isHashed(value),
                    "test data is wrong: " + value + " was expected to be plain text");
            long before = countInDatabase();
            insertUserWithPassword(value);
            assertEquals(before + 1, countInDatabase(),
                    "the query must count " + value + " as plain text, as the migration does");
        }

        for (String value : alreadyHashed) {
            assertTrue(passwordService.isHashed(value),
                    "test data is wrong: " + value + " was expected to be a BCrypt hash");
            long before = countInDatabase();
            insertUserWithPassword(value);
            assertEquals(before, countInDatabase(),
                    "the query must not count an existing hash as plain text, or the operator "
                            + "would be told to migrate a table that is already migrated");
        }
    }

    @Test
    void aBlankPasswordIsNotCountedAsPlainText() throws Exception {
        requireTestDatabase();

        for (String value : new String[] { "", "   " }) {
            assertTrue(value.isBlank(), "test data is wrong: expected a blank value");
            long before = countInDatabase();
            insertUserWithPassword(value);
            assertEquals(before, countInDatabase(),
                    "a blank password has nothing to hash, so it must not be counted");
        }
    }

    @Test
    void theQueryAgreesWithACountComputedInJava() throws Exception {
        requireTestDatabase();

        long javaBefore = countInJava();
        long databaseBefore = countInDatabase();

        insertUserWithPassword("java.agreement.plain");
        insertUserWithPassword(VALID_HASH);
        insertUserWithPassword("java.agreement.also.plain");

        assertEquals(countInJava() - javaBefore, countInDatabase() - databaseBefore,
                "the SQL definition and the Java definition of 'still plain text' have drifted "
                        + "apart; the deployment verification query can no longer be trusted");
    }
}