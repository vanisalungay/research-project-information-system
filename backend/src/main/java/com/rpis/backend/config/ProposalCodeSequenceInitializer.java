package com.rpis.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the PostgreSQL sequence used to allocate proposal codes exists and is
 * aligned with the codes already present in the database.
 * <p>
 * Proposal codes are allocated with {@code nextval('proposal_code_seq')} so that
 * simultaneous submissions from any number of backend instances always receive a
 * distinct sequential number. This runs once after the application context is
 * ready and is idempotent: it never renumbers or deletes existing records, and it
 * only ever moves the sequence forward.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProposalCodeSequenceInitializer {

    private static final String SEQUENCE = "proposal_code_seq";

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSequence() {
        try {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS " + SEQUENCE);

            Long maxCode = jdbcTemplate.queryForObject(
                    "SELECT MAX(CAST(proposal_code AS INTEGER)) FROM proposals "
                            + "WHERE proposal_code ~ '^[0-9]+$'",
                    Long.class);

            if (maxCode == null) {
                // No numeric codes yet -> the next allocation must be 001.
                jdbcTemplate.queryForObject("SELECT setval('" + SEQUENCE + "', 1, false)", Long.class);
            } else {
                Long next = jdbcTemplate.queryForObject("SELECT nextval('" + SEQUENCE + "')", Long.class);
                if (next == null || next <= maxCode) {
                    // Bring the sequence just past the highest existing code so a
                    // newly generated id can never collide with an existing one.
                    jdbcTemplate.queryForObject(
                            "SELECT setval('" + SEQUENCE + "', " + maxCode + ", true)", Long.class);
                }
            }
            log.info("proposal_code_seq is ready (highest existing code: {})", maxCode);
        } catch (Exception e) {
            log.warn("Failed to initialize {}: {}", SEQUENCE, e.getMessage());
        }
    }
}
