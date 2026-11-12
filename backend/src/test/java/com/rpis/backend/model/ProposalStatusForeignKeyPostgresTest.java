package com.rpis.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PostgreSQL-backed tests for the three proposal-status relationships introduced by
 * {@code migration-norm-04-proposal-status-fks.sql}:
 *
 * <pre>
 *   proposals.status                     -> proposal_statuses.code
 *   proposal_status_history.from_status  -> proposal_statuses.code
 *   proposal_status_history.to_status    -> proposal_statuses.code
 * </pre>
 *
 * <p>The test profile builds its schema from the JPA entities, and
 * {@code proposal_statuses} has no entity, so the test creates that reference table and
 * the exact three constraints before exercising them. The migration itself is applied to
 * the live database separately; its post-apply verification query checks the real
 * catalog. Here the point is the behaviour the constraints guarantee: every non-NULL
 * status must resolve to a lookup code, while a NULL {@code from_status} remains valid
 * for baseline history rows.</p>
 *
 * <p>Runs against the isolated {@code rpis_test} database only. The DDL and every row the
 * test writes are rolled back by the test transaction.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalStatusForeignKeyPostgresTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long proposalId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposal_status_history RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");

        // The reference table is created by migration-03, not by Hibernate. Recreate the
        // same shape here so the constraints can be tested on the disposable test schema.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.proposal_statuses (
                    code         VARCHAR(50) PRIMARY KEY,
                    stage_label  VARCHAR(80) NOT NULL,
                    stage_order  INTEGER     NOT NULL,
                    bucket       VARCHAR(30),
                    active       BOOLEAN     NOT NULL DEFAULT TRUE,
                    notes        TEXT
                )
                """);

        // The statuses these tests use. 'FOR REVIEW' is the current application spelling
        // of the submission state; 'SUBMITTED' is the legacy spelling that existing rows
        // still carry. Both must resolve to a lookup code.
        insertStatus("DRAFT", "Draft");
        insertStatus("SUBMITTED", "Submitted to RPS");
        insertStatus("FOR REVIEW", "For RPS Review");
        insertStatus("UNDER_REVIEW", "Under REC Evaluation");
        insertStatus("APPROVED", "Approved");

        // The same definitions migration-norm-04 adds. Guarded so a repeated @BeforeEach
        // within one context is harmless.
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                                    WHERE conname = 'fk_proposals_status'
                                      AND connamespace = 'public'::regnamespace) THEN
                        ALTER TABLE public.proposals
                            ADD CONSTRAINT fk_proposals_status
                            FOREIGN KEY (status)
                            REFERENCES public.proposal_statuses(code)
                            ON DELETE RESTRICT ON UPDATE NO ACTION;
                    END IF;

                    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                                    WHERE conname = 'fk_psh_from_status'
                                      AND connamespace = 'public'::regnamespace) THEN
                        ALTER TABLE public.proposal_status_history
                            ADD CONSTRAINT fk_psh_from_status
                            FOREIGN KEY (from_status)
                            REFERENCES public.proposal_statuses(code)
                            ON DELETE RESTRICT ON UPDATE NO ACTION;
                    END IF;

                    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                                    WHERE conname = 'fk_psh_to_status'
                                      AND connamespace = 'public'::regnamespace) THEN
                        ALTER TABLE public.proposal_status_history
                            ADD CONSTRAINT fk_psh_to_status
                            FOREIGN KEY (to_status)
                            REFERENCES public.proposal_statuses(code)
                            ON DELETE RESTRICT ON UPDATE NO ACTION;
                    END IF;
                END $$;
                """);

        proposalId = jdbcTemplate.queryForObject(
                "INSERT INTO public.proposals (project_title, status) "
                        + "VALUES ('NORM-04 foreign-key test', 'FOR REVIEW') RETURNING id",
                Long.class);
    }

    private void insertStatus(String code, String label) {
        jdbcTemplate.update("""
                INSERT INTO public.proposal_statuses (code, stage_label, stage_order, bucket, active, notes)
                VALUES (?, ?, 10, 'test', TRUE, 'NORM-04 test fixture')
                ON CONFLICT (code) DO NOTHING
                """, code, label);
    }

    @Test
    void aProposalStatusMustExistInTheLookup() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "UPDATE public.proposals SET status = 'NOT_A_PROPOSAL_STATUS' WHERE id = ?",
                        proposalId));
    }

    @Test
    void aHistoryFromStatusMustExistInTheLookup() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO public.proposal_status_history "
                                + "(proposal_id, from_status, to_status, source, changed_at) "
                                + "VALUES (?, 'NOT_A_PROPOSAL_STATUS', 'DRAFT', 'APPLICATION', CURRENT_TIMESTAMP)",
                        proposalId));
    }

    @Test
    void aHistoryToStatusMustExistInTheLookup() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO public.proposal_status_history "
                                + "(proposal_id, from_status, to_status, source, changed_at) "
                                + "VALUES (?, 'DRAFT', 'NOT_A_PROPOSAL_STATUS', 'APPLICATION', CURRENT_TIMESTAMP)",
                        proposalId));
    }

    @Test
    void bothSubmissionSpellingsAreAccepted() {
        assertEquals(1, jdbcTemplate.update(
                "INSERT INTO public.proposal_status_history "
                        + "(proposal_id, from_status, to_status, source, changed_at) "
                        + "VALUES (?, 'DRAFT', 'FOR REVIEW', 'APPLICATION', CURRENT_TIMESTAMP)",
                proposalId));
        assertEquals(1, jdbcTemplate.update(
                "INSERT INTO public.proposal_status_history "
                        + "(proposal_id, from_status, to_status, source, changed_at) "
                        + "VALUES (?, 'SUBMITTED', 'FOR REVIEW', 'APPLICATION', CURRENT_TIMESTAMP)",
                proposalId));
    }

    @Test
    void aNullFromStatusIsStillValidForBaselineHistoryRows() {
        assertEquals(1, jdbcTemplate.update(
                "INSERT INTO public.proposal_status_history "
                        + "(proposal_id, from_status, to_status, source, changed_at) "
                        + "VALUES (?, NULL, 'DRAFT', 'BASELINE_SEED', CURRENT_TIMESTAMP)",
                proposalId));
    }

    @Test
    void aNullProposalStatusIsStillValid() {
        // The column is nullable and carries a DRAFT default. The FK must not turn a
        // previously valid NULL into an error.
        assertEquals(1, jdbcTemplate.update(
                "UPDATE public.proposals SET status = NULL WHERE id = ?", proposalId));
    }
}
