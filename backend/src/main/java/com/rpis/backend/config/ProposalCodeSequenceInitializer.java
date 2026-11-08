package com.rpis.backend.config;

import com.rpis.backend.util.SchemaVerification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the PostgreSQL sequence used to allocate proposal codes exists and is aligned
 * with the codes already present in the database.
 *
 * <h2>Schema changes are a migration, not a start-up step</h2>
 * CREATE SEQUENCE and setval used to run on every application start. The sequence and the
 * same alignment statement are already shipped as
 * backend/migration-add-proposal-code-sequence.sql, so a start-up now only verifies that the
 * sequence exists, with two SELECT statements, unless
 * {@code rpis.bootstrap.sequences.enabled} (environment variable
 * {@code RPIS_BOOTSTRAP_SEQUENCES_ENABLED}) is set to true explicitly - which an operator
 * does once, for a new database or after a manual data import that inserted explicit codes.
 *
 * <h2>Concurrency and direction of alignment</h2>
 * Proposal codes are allocated with {@code nextval('proposal_code_seq')}, which is atomic,
 * so simultaneous submissions from any number of instances always receive a distinct number.
 * The alignment itself is a <em>single</em> setval statement no longer preceded by a probe
 * nextval, and it is written as
 * {@code GREATEST(<current last_value>, MAX(stored code) + 1)}. That has two consequences
 * that matter here:
 * <ul>
 *   <li><strong>the sequence can never move backwards</strong>, so a code that was already
 *       handed out can never be handed out again - not even when stored proposals were
 *       deleted since the sequence advanced;</li>
 *   <li><strong>it can never move below the highest stored code + 1</strong>, so a newly
 *       generated code cannot collide with an identifier already in the table.</li>
 * </ul>
 * Two instances running the alignment at the same time compute the same target value and
 * write it, which is idempotent; no advisory lock is needed because there is no
 * check-then-insert race left in the statement (unlike the agency seed, which inserts rows).
 * On a completely fresh sequence with no stored codes, and only then, the first allocation
 * is set to 001.
 */
@Slf4j
@Component
public class ProposalCodeSequenceInitializer {

    private static final String SEQUENCE = "proposal_code_seq";

    /** The file that documents the DDL and the alignment applied when explicitly enabled. */
    public static final String MIGRATION_FILE = "backend/migration-add-proposal-code-sequence.sql";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Whether this start-up may create the sequence and align it. False unless
     * rpis.bootstrap.sequences.enabled=true.
     */
    private final boolean enabled;

    /**
     * The constructor is written out explicitly instead of relying on
     * {@code @RequiredArgsConstructor}. Lombok-injected constructors are currently
     * misreported by the Eclipse/JDT based Java language server as "The Java feature
     * 'Flexible Constructor Bodies' is only available with source level 25 and above"
     * (false positive, see vscode-java#4521), so an explicit constructor keeps this class
     * free of that diagnostic.
     */
    public ProposalCodeSequenceInitializer(JdbcTemplate jdbcTemplate,
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
            // A sequence that has never been used, with nothing stored: the first allocation
            // must be 001 rather than 002. This is the one branch that is not expressed as a
            // GREATEST, and it cannot move the sequence backwards because is_called was false.
            jdbcTemplate.queryForObject("SELECT setval('" + SEQUENCE + "', 1, false)", Long.class);
            log.info("{} is ready; the next proposal code allocated will be 001.", SEQUENCE);
            return;
        }

        // One statement, so there is no window between reading the sequence and writing it
        // back. GREATEST keeps the value from moving backwards and from sitting below the
        // highest stored code.
        Long aligned = jdbcTemplate.queryForObject(
                "SELECT setval('" + SEQUENCE + "', GREATEST("
                        + "COALESCE((SELECT last_value FROM " + SEQUENCE + "), 1), "
                        + "COALESCE((SELECT MAX(CAST(proposal_code AS INTEGER)) FROM proposals "
                        + "WHERE proposal_code ~ '" + "^[0-9]+$" + "'), 0) + 1), true)",
                Long.class);
        log.info("{} is ready: aligned to {} (highest stored code: {}). The sequence is never moved "
                + "backwards.", SEQUENCE, aligned, maxCode);
    }

    /** The highest numeric proposal code stored, or null when none is. A single SELECT. */
    private Long highestStoredCode() {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(proposal_code AS INTEGER)) FROM proposals "
                        + "WHERE proposal_code ~ '" + "^[0-9]+$" + "'",
                Long.class);
    }

    /**
     * Reports whether the sequence exists, and how far the stored codes have advanced,
     * without creating the sequence or moving it. SELECT statements only.
     */
    private void verifyWithoutChangingAnything() {
        try {
            if (!SchemaVerification.relationExists(jdbcTemplate, SEQUENCE)) {
                log.warn("The sequence {} is MISSING and schema bootstrap is disabled "
                        + "(rpis.bootstrap.sequences.enabled=false, environment variable "
                        + "RPIS_BOOTSTRAP_SEQUENCES_ENABLED). Proposal creation inserts nextval({}) and "
                        + "will fail until it exists: apply {} or start once with that variable set to "
                        + "true. Nothing was changed by this instance.", SEQUENCE, SEQUENCE, MIGRATION_FILE);
                return;
            }
            Long maxCode = highestStoredCode();
            log.info("{} verified read-only: the sequence exists and {} has/have been stored. Schema "
                    + "bootstrap is disabled, so nothing was created, advanced or aligned.",
                    SEQUENCE, maxCode == null ? "no numeric proposal codes" : (maxCode + " numeric proposal codes"));
        } catch (Exception e) {
            log.warn("Could not verify the sequence {} ({}). Nothing was changed.", SEQUENCE, e.getMessage());
        }
    }
}