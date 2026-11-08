package com.rpis.backend.config;

import com.rpis.backend.util.SchemaVerification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the PostgreSQL sequence used to allocate endorsement references exists and is
 * aligned with the references already stored.
 *
 * <h2>Schema changes are a migration, not a start-up step</h2>
 * CREATE SEQUENCE and setval used to run on every application start. The sequence and the
 * same alignment statement are already shipped as
 * backend/migration-add-endorsement-workflow.sql, so a start-up now only verifies that the
 * sequence exists, with SELECT statements, unless rpis.bootstrap.sequences.enabled
 * (environment variable RPIS_BOOTSTRAP_SEQUENCES_ENABLED) is set to true explicitly.
 *
 * <h2>Concurrency and direction of alignment</h2>
 * References are allocated with nextval(endorsement_code_seq), which is atomic, so
 * simultaneous requests from any number of instances always receive a distinct, increasing
 * reference. The alignment is a single setval written as
 * GREATEST(current last_value, MAX(stored reference) + 1), so it can never move the sequence
 * backwards and never below the highest stored reference. No advisory lock is required:
 * there is no check-then-insert race left - the statement computes and writes its value
 * atomically, and two instances running it write the same value. Mirrors
 * ProposalCodeSequenceInitializer.
 */
@Slf4j
@Component
public class EndorsementCodeSequenceInitializer {

    private static final String SEQUENCE = "endorsement_code_seq";

    /** The reference format whose numeric part the sequence follows. */
    private static final String REFERENCE_PATTERN = "^MSUN-ORPS-END-[0-9]+-[0-9]{4}$";

    /** The file that documents the DDL and the alignment applied when explicitly enabled. */
    public static final String MIGRATION_FILE = "backend/migration-add-endorsement-workflow.sql";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Whether this start-up may create the sequence and align it. False unless
     * rpis.bootstrap.sequences.enabled=true.
     */
    private final boolean enabled;

    public EndorsementCodeSequenceInitializer(JdbcTemplate jdbcTemplate,
            @Value("${rpis.bootstrap.sequences.enabled:false}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSequence() {
        if (!enabled) {
            verifyWithoutChangingAnything();
            return;
        }
        applySequence();
    }

    /**
     * Creates the sequence when missing and aligns it forward. Private on purpose: the only
     * caller is the gate above, so no other code path can run DDL or advance the sequence by
     * forgetting to check the property.
     */
    private void applySequence() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS " + SEQUENCE);

        Boolean called = jdbcTemplate.queryForObject("SELECT is_called FROM " + SEQUENCE, Boolean.class);
        Long maxCode = highestStoredCode();

        if (maxCode == null && !Boolean.TRUE.equals(called)) {
            // Never used, nothing stored: the first reference must be END-001. This branch
            // cannot move the sequence backwards, because is_called was false.
            jdbcTemplate.queryForObject("SELECT setval('" + SEQUENCE + "', 1, false)", Long.class);
            log.info("{} is ready; the next endorsement reference allocated will be 001.", SEQUENCE);
            return;
        }

        // One statement, so there is no window between reading the sequence and writing it
        // back. GREATEST keeps the value from moving backwards and from sitting below the
        // highest stored reference.
        Long aligned = jdbcTemplate.queryForObject(
                "SELECT setval('" + SEQUENCE + "', GREATEST("
                        + "COALESCE((SELECT last_value FROM " + SEQUENCE + "), 1), "
                        + "COALESCE((SELECT MAX(CAST(substring(reference_number FROM "
                        + "'END-([0-9]+)-') AS INTEGER)) FROM endorsement_requests "
                        + "WHERE reference_number ~ '" + REFERENCE_PATTERN + "'), 0) + 1), true)",
                Long.class);
        log.info("{} is ready: aligned to {} (highest stored reference: {}). The sequence is never "
                + "moved backwards.", SEQUENCE, aligned, maxCode);
    }

    /** The highest numeric endorsement reference stored, or null when none is. One SELECT. */
    private Long highestStoredCode() {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(substring(reference_number FROM 'END-([0-9]+)-') AS INTEGER)) "
                        + "FROM endorsement_requests "
                        + "WHERE reference_number ~ '" + REFERENCE_PATTERN + "'",
                Long.class);
    }

    /**
     * Reports whether the sequence exists without creating or moving it. SELECT statements
     * only.
     */
    private void verifyWithoutChangingAnything() {
        try {
            if (!SchemaVerification.relationExists(jdbcTemplate, SEQUENCE)) {
                log.warn("The sequence {} is MISSING and schema bootstrap is disabled "
                        + "(rpis.bootstrap.sequences.enabled=false, environment variable "
                        + "RPIS_BOOTSTRAP_SEQUENCES_ENABLED). Issuing a new endorsement needs nextval({}): "
                        + "apply {} or start once with that variable set to true. Nothing was changed by "
                        + "this instance.", SEQUENCE, SEQUENCE, MIGRATION_FILE);
                return;
            }
            Long maxCode = highestStoredCode();
            log.info("{} verified read-only: the sequence exists and {} stored reference(s) were found. "
                    + "Schema bootstrap is disabled, so nothing was created, advanced or aligned.",
                    SEQUENCE, maxCode == null ? Long.valueOf(0) : maxCode);
        } catch (Exception e) {
            log.warn("Could not verify the sequence {} ({}). Nothing was changed.", SEQUENCE, e.getMessage());
        }
    }
}