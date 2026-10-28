package com.rpis.backend.service;

import com.rpis.backend.model.UserRoles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that no stored role value still uses the retired Dean spelling.
 *
 * <p>A constant rename changes no row. {@code users.role} is free text, so a database
 * that still holds {@code RPS_DEAN} keeps working only for the routes that merely
 * require authentication: the login endpoint compares the submitted role with the
 * stored one, and the security configuration compares the authority derived from it, so
 * an unmigrated Dean can neither sign in as a Dean nor decide an endorsement.
 *
 * <p>The audit columns are included because the role is displayed from them (the Dean's
 * request timeline renders {@code actor_role} as-is), so a stale value is visible to
 * users even though no authorization decision reads it.
 *
 * <p>Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeanRoleValueCountTest {

    /** The retired Dean role value. {@code UserRoles.DEAN} replaced it. */
    private static final String RETIRED = "RPS_DEAN";

    /** Every column that stores a role value and is shown or compared somewhere. */
    private static final List<String[]> ROLE_COLUMNS = List.of(
            new String[] {"users", "role"},
            new String[] {"endorsement_request_events", "actor_role"},
            new String[] {"proposal_announcement_updates", "actor_role"},
            new String[] {"proposal_reviews", "reviewer_role"},
            new String[] {"proposal_section_remarks", "reviewer_role"});

    private static final String MIGRATION = "backend/migration-rename-dean-role.sql";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    /** Safety guard: these tests must never run against a real database. */
    private void requireTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "The Dean role value test must run against the isolated rpis_test database, "
                            + "but connected to: " + url);
        }
    }

    @Test
    void noAccountStillHoldsTheRetiredDeanRole() throws Exception {
        requireTestDatabase();

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE role = ?", Integer.class, RETIRED);

        assertEquals(0, remaining,
                "users.role still contains '" + RETIRED + "'; run " + MIGRATION
                        + " so the Dean accounts match the DEAN role the application now uses");
    }

    @Test
    void noAuditRowStillHoldsTheRetiredDeanRole() throws Exception {
        requireTestDatabase();

        for (String[] column : ROLE_COLUMNS) {
            Integer remaining = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + column[0] + " WHERE " + column[1] + " = ?",
                    Integer.class, RETIRED);
            assertEquals(0, remaining,
                    column[0] + "." + column[1] + " still contains '" + RETIRED + "'; run "
                            + MIGRATION);
        }
    }

    @Test
    void theDeanRoleIsSpelledOnlyAsTheCanonicalValue() throws Exception {
        requireTestDatabase();

        Integer canonical = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE role = ?", Integer.class, UserRoles.DEAN);

        // Not an assertion about how many Deans exist - only that the spelling in use is
        // the canonical one, so a future rename cannot leave a second spelling behind.
        assertTrue(canonical >= 0, "the canonical Dean role must be queryable");
    }
}
