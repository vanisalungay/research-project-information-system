-- ===========================================================================
-- RPIS database audit - Migration 03 of 03: status reference + status history
-- ===========================================================================
-- Creates the status reference table and the status-history audit table, then
-- initialises ONE honest baseline row per existing proposal.
--
-- APPROVED SCOPE FOR THIS FILE (do not extend without approval)
--   proposal_statuses          28 seeded status rows
--   proposal_status_history    26 BASELINE_SEED rows (one per proposal)
--   2 foreign keys, 1 explicit index, 2 primary-key indexes
--   Nothing is dropped. 0 existing rows are updated or deleted.
--   Exactly two foreign keys are created, both on proposal_status_history.
--   No college/department foreign key. No foreign key for
--   endorsement_request_events.actor_id. notifications.proposal_id untouched.
--
-- WHAT IS DELIBERATELY **NOT** DONE HERE
--   No historical transition is reconstructed or invented. proposal_versions
--   cannot support a reconstruction, verified from the data:
--     * its rows were rewritten by the backfill runner (created_at 2026-09-06
--       for submissions dated July 2026; 11 of 22 proposals have created_at on a
--       different date from submission_date);
--     * every proposal carries a single distinct version status, including the two
--       proposals that have 2 and 4 version rows;
--     * the version statuses (SUBMITTED 20, ENDORSED 7, FOR_OVCAF_APPROVAL 4,
--       REJECTED 3, APPROVED 1) contradict the live proposals.status values
--       (SUBMITTED 5, DRAFT 4, APPROVED 6, ...).
--   The only genuine per-version historical evidence is submission_date and
--   revision_date. Each existing proposal therefore gets exactly one row:
--   from_status NULL, to_status = the CURRENT status, source='BASELINE_SEED'.
--
-- WHAT MUST FOLLOW IN APPLICATION CODE (not a database change)
--   New status changes must be recorded transactionally by every code path that
--   changes proposals.status. Until that is deployed this table holds baseline
--   rows only for historical proposals and NO transitions for new ones.
--
-- COVERAGE GUARANTEE
--   The baseline INSERT is guarded by NOT EXISTS on source='BASELINE_SEED', so a
--   second run inserts 0 rows and never duplicates. A proposal created later is
--   covered by the application, not by re-running this file.
--
-- PRESERVED DECISIONS (verified intact in this file)
--   * endorsement_request_events ids 11 and 12 are NOT modified.
--   * No FK for endorsement_request_events.actor_id.
--   * No notifications.proposal_id foreign key is created.
--   * notifications.proposal_id is NOT rewritten.
--   * No college/department foreign key appears anywhere.
--   * Existing audit records are preserved: this file updates 0 existing rows.
--   * No college, department or agency data is invented.
--
-- DOLLAR-QUOTE TAGS: every block uses a unique tag ($m03_fks$) for the same
-- reason documented in migration 01 - identical tags cannot nest.
--
-- Preconditions verified against the rpis_db snapshot of 2026-11-08
--   (26 proposals, all with a non-NULL status; created_at and updated_at non-NULL
--   for all 26).
--
-- Apply with:
--   psql -v ON_ERROR_STOP=1 -h localhost -U postgres -d <database> \
--        -f migration-03-db-audit-status-history.sql
-- ===========================================================================

\set ON_ERROR_STOP on
BEGIN;

-- ===========================================================================
-- SECTION 1 - STATUS REFERENCE TABLE
-- ---------------------------------------------------------------------------
-- code        - the value actually stored in proposals.status. NO stored status
--               is rewritten by this migration.
-- stage_label - the human-readable stage, mapping the implemented vocabulary onto
--               the stage names the UI and the documentation use.
-- bucket      - the Proponent dashboard bucket (src/utils/proposalSummary.js).
-- ON CONFLICT DO NOTHING makes the seed idempotent and preserves any later
-- administrator edit.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS public.proposal_statuses (
    code         VARCHAR(50) PRIMARY KEY,
    stage_label  VARCHAR(80)  NOT NULL,
    stage_order  INTEGER      NOT NULL,
    bucket       VARCHAR(30),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    notes        TEXT
);
INSERT INTO public.proposal_statuses (code, stage_label, stage_order, bucket, active, notes) VALUES
    ('DRAFT',                 'Draft',                    10, 'draft',       TRUE, 'Being prepared by the Proponent.'),
    ('SUBMITTED',             'Submitted to RPS',         20, 'submitted',   TRUE, 'Awaiting the RPS completeness review (RPS Staff or RPS Director).'),
    ('RPS_RETURNED',          'Returned by RPS',          30, 'returned',    TRUE, 'Returned to the Proponent by RPS for revision.'),
    ('RETURNED_TO_RPS',       'Returned to RPS',          35, 'returned',    TRUE, 'Returned by a reviewing office; RPS forwards it to the Proponent.'),
    ('ENDORSED',              'Endorsed to OVCRIGE',      40, 'underReview', TRUE, 'RPS endorsed it for OVCRIGE coordination.'),
    ('OVCRIGE_COORDINATION',  'OVCRIGE Coordination',     45, 'underReview', TRUE, 'Stage name used by the monitoring timeline; not written by the current backend.'),
    ('UNDER_REVIEW',          'Under REC Evaluation',     50, 'underReview', TRUE, 'Forwarded to the REC for technical and merit evaluation.'),
    ('REC_EVALUATION',        'REC Evaluation',           55, 'underReview', TRUE, 'Stage name used by the monitoring timeline; not written by the current backend.'),
    ('REC_APPROVED',          'Approved by REC',          60, 'underReview', TRUE, 'REC passed it; awaiting the Chancellor-stage processing.'),
    ('OVC_APPROVED',          'Approved by OVC',          65, 'underReview', TRUE, 'Approved by the OVC review step.'),
    ('CHANCELLOR_REVIEW',     'Chancellor Review',        70, 'underReview', TRUE, 'Stage name used by the monitoring timeline; not written by the current backend.'),
    ('FOR_OC_APPROVAL',       'For Chancellor Approval',  75, 'underReview', TRUE, 'Awaiting the Office of the Chancellor final approval and Special Order.'),
    ('FOR_OVCAF_APPROVAL',    'Finance Authorization',    80, 'underReview', TRUE, 'Awaiting OVCAF budget endorsement and fund release.'),
    ('FINANCE_AUTHORIZATION', 'Finance Authorization',    85, 'underReview', TRUE, 'Stage name used by the monitoring timeline; not written by the current backend.'),
    ('READY_FOR_RELEASE',     'Ready for Fund Release',   90, 'underReview', TRUE, 'OVCAF endorsed the budget; awaiting release of funds.'),
    ('RELEASED',              'Funds Released',           95, 'approved',    TRUE, 'Funds released; the project enters implementation.'),
    ('IMPLEMENTATION',        'Implementation',          100, 'approved',    TRUE, 'Implementation phase. The current backend infers this from so_number IS NOT NULL.'),
    ('APPROVED',              'Approved',                105, 'approved',    TRUE, 'Finally approved (Special Order issued; no budget step required).'),
    ('APPROVED_ENDORSED',     'Approved and Endorsed',   110, 'approved',    TRUE, 'Legacy OVCAF decision value found in the front-end bucket list.'),
    ('COMPLETED',             'Completed',               120, 'approved',    TRUE, 'Reporting finished and reviewed. Not written by the current backend.'),
    ('REJECTED',              'Rejected',                130, 'returned',    TRUE, 'Rejected.'),
    ('REC_REJECTED',          'Rejected by REC',         135, 'returned',    TRUE, 'Rejected by the REC.'),
    ('REVISION',              'For Revision',            140, 'returned',    TRUE, 'Revision required; settable only through the RPS revision workflow.'),
    ('REC_REVISION',          'REC Revision',            145, 'returned',    TRUE, 'Revision required by the REC; settable only through the RPS revision workflow.'),
    ('PENDING_REVISION',      'Pending Revision',        150, 'returned',    TRUE, 'Revision pending; settable only through the RPS revision workflow.'),
    ('RETURNED',              'Returned by OVCAF',       155, 'returned',    TRUE, 'Returned by OVCAF for a budget revision.'),
    ('RETURNED_FOR_REVISION', 'Returned for Revision',   160, 'returned',    TRUE, 'Returned for revision.'),
    ('RPS_REVIEW',            'RPS Review',              165, 'underReview', TRUE, 'Stage name used by the monitoring timeline; not written by the current backend.')
ON CONFLICT (code) DO NOTHING;
-- ===========================================================================
-- SECTION 2 - STATUS HISTORY TABLE
-- ===========================================================================
CREATE TABLE IF NOT EXISTS public.proposal_status_history (
    id            BIGSERIAL    PRIMARY KEY,
    proposal_id   BIGINT       NOT NULL,
    from_status   VARCHAR(50),
    to_status     VARCHAR(50)  NOT NULL,
    actor_user_id BIGINT,
    actor_role    VARCHAR(50),
    actor_name    VARCHAR(255),
    remarks       TEXT,
    source        VARCHAR(30)  NOT NULL DEFAULT 'APPLICATION',
    changed_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Foreign keys are created in a separate guarded block: CREATE TABLE IF NOT EXISTS
-- skips the WHOLE statement, including inline constraints, when the table already
-- exists. That is exactly the defect that left quarterly_progress_reports without
-- the foreign keys its own migration declared.
DO $m03_fks$
DECLARE
    entry record;
BEGIN
    FOR entry IN
        SELECT * FROM (VALUES
            ('fk_psh_proposal',   'proposal_status_history', 'proposal_id',   'proposals', 'CASCADE'),
            ('fk_psh_actor_user', 'proposal_status_history', 'actor_user_id', 'users',     'SET NULL')
        ) AS t(conname, child_table, child_col, parent_table, on_delete)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
              JOIN pg_class     t  ON t.oid = c.conrelid
              JOIN pg_namespace ns ON ns.oid = t.relnamespace
             WHERE c.conname = entry.conname AND ns.nspname = 'public')
        THEN
            EXECUTE format(
                'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) '
                'REFERENCES public.%I(id) ON DELETE %s',
                entry.child_table, entry.conname, entry.child_col,
                entry.parent_table, entry.on_delete);
        END IF;
    END LOOP;
END $m03_fks$;

-- ON DELETE: proposal_id is CASCADE because the history is part of the proposal
-- aggregate; RESTRICT would break the existing "delete my own draft" flow, since
-- every proposal receives a baseline row. actor_user_id is SET NULL so a status
-- history row is never destroyed along with an account.

CREATE INDEX IF NOT EXISTS idx_psh_proposal_changed
    ON public.proposal_status_history (proposal_id, changed_at);

-- ===========================================================================
-- SECTION 3 - HONEST BASELINE (one row per existing proposal, current state only)
-- ---------------------------------------------------------------------------
-- Idempotent through NOT EXISTS on source = BASELINE_SEED. Re-running inserts 0
-- rows. changed_at falls back to created_at, which is non-NULL for all 26
-- proposals (verified).
-- ===========================================================================
INSERT INTO public.proposal_status_history
       (proposal_id, from_status, to_status, actor_user_id, actor_role, actor_name,
        remarks, source, changed_at)
SELECT p.id,
       NULL,
       p.status,
       NULL,
       NULL,
       NULL,
       'Baseline row initialised from the proposal row state at migration time. '
       'This is NOT a reconstruction of the proposal''s historical transitions: '
       'no reliable chronological status history exists in the database.',
       'BASELINE_SEED',
       COALESCE(p.updated_at, p.created_at, CURRENT_TIMESTAMP)
  FROM public.proposals p
 WHERE p.status IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM public.proposal_status_history h
        WHERE h.proposal_id = p.id AND h.source = 'BASELINE_SEED');
COMMIT;

-- ===========================================================================
-- VERIFICATION - this file's own objects only.
-- ===========================================================================
SELECT 'M03 ASSERTION - tables present (expected 2)' AS check_name, count(*)::text AS value
  FROM information_schema.tables
 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
   AND table_name IN ('proposal_statuses','proposal_status_history')
UNION ALL
SELECT 'M03 ASSERTION - foreign keys present (expected 2)', count(*)::text
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace AND contype = 'f'
   AND conname IN ('fk_psh_proposal','fk_psh_actor_user')
UNION ALL
SELECT 'M03 ASSERTION - index entries present (expected 3 = 1 explicit + 2 primary-key)',
       count(*)::text
  FROM pg_indexes
 WHERE schemaname = 'public'
   AND indexname IN ('idx_psh_proposal_changed','proposal_statuses_pkey','proposal_status_history_pkey')
UNION ALL
SELECT 'M03 ASSERTION - proposal_statuses rows (expected 28)', count(*)::text
  FROM public.proposal_statuses
UNION ALL
SELECT 'M03 ASSERTION - BASELINE_SEED rows (expected 26 = proposals with a status)',
       count(*)::text
  FROM public.proposal_status_history WHERE source = 'BASELINE_SEED'
UNION ALL
SELECT 'M03 ASSERTION - distinct proposals covered (expected 26)', count(DISTINCT proposal_id)::text
  FROM public.proposal_status_history
UNION ALL
SELECT 'M03 INFO - rows NOT BASELINE_SEED (expected 0 until application code records transitions)',
       count(*)::text
  FROM public.proposal_status_history WHERE source IS DISTINCT FROM 'BASELINE_SEED';
