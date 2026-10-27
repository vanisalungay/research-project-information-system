package com.rpis.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the PostgreSQL sequence used to allocate endorsement references exists and
 * is aligned with the references already stored.
 *
 * <p>References are allocated with {@code nextval('endorsement_code_seq')} so that
 * simultaneous requests from any number of backend instances always receive a
 * distinct, increasing reference. This runs once after the application context is
 * ready and is idempotent: it never renumbers or deletes existing records and only
 * ever moves the sequence forward. Mirrors
 * {@link ProposalCodeSequenceInitializer}, which does the same for proposal codes.
 */
@Slf4j
@Component
public class EndorsementCodeSequenceInitializer {

    private static final String SEQUENCE = "endorsement_code_seq";

    private final JdbcTemplate jdbcTemplate;

    public EndorsementCodeSequenceInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSequence() {
        try {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS " + SEQUENCE);

            Long maxCode = jdbcTemplate.queryForObject(
                    "SELECT MAX(CAST(substring(reference_number FROM 'END-([0-9]+)-') AS INTEGER)) "
                            + "FROM endorsement_requests "
                            + "WHERE reference_number ~ '^MSUN-ORPS-END-[0-9]+-[0-9]{4}$'",
                    Long.class);

            if (maxCode == null) {
                // No references yet -> the next allocation must be 001.
                jdbcTemplate.queryForObject("SELECT setval('" + SEQUENCE + "', 1, false)", Long.class);
            } else {
                Long next = jdbcTemplate.queryForObject("SELECT nextval('" + SEQUENCE + "')",
                        Long.class);
                if (next == null || next <= maxCode) {
                    jdbcTemplate.queryForObject(
                            "SELECT setval('" + SEQUENCE + "', " + maxCode + ", true)", Long.class);
                }
            }
            log.info("endorsement_code_seq is ready (highest existing reference: {})", maxCode);
        } catch (Exception e) {
            log.warn("Failed to initialize {}: {}", SEQUENCE, e.getMessage());
        }
    }
}
