-- =============================================================================
-- Rollback for NORM-01 (relational integrity - 15 foreign keys) - rpis_db
-- =============================================================================
-- Reverses ONLY the objects created by migration-norm-01-relational-integrity.sql
-- Data-safe: constraints and indexes carry no data; no row is touched.
-- Safe to run repeatedly (every step is guarded).
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

-- 1) drop the unique candidate key
DO $rb01_uq$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uq_proposal_priority_agendas'
                 AND connamespace='public'::regnamespace) THEN
        ALTER TABLE public.proposal_priority_agendas DROP CONSTRAINT uq_proposal_priority_agendas;
    END IF;
END $rb01_uq$;

-- 2) drop the 11 indexes
DROP INDEX IF EXISTS public.idx_qpr_created_by;
DROP INDEX IF EXISTS public.idx_qpr_submitted_by;
DROP INDEX IF EXISTS public.idx_project_reports_submitted_by;
DROP INDEX IF EXISTS public.idx_project_reports_reviewed_by;
DROP INDEX IF EXISTS public.idx_psr_reviewer;
DROP INDEX IF EXISTS public.idx_pau_actor;
DROP INDEX IF EXISTS public.idx_proposals_returned_by;
DROP INDEX IF EXISTS public.idx_proposals_revision_forwarded_by;
DROP INDEX IF EXISTS public.idx_ovcaf_released_by;
DROP INDEX IF EXISTS public.idx_dean_assignments_created_by;
DROP INDEX IF EXISTS public.idx_dean_assignments_revoked_by;

-- 3) drop the 15 foreign keys (child table resolved from the catalog)
DO $rb01_fks$
DECLARE c record;
BEGIN
    FOR c IN SELECT conname, conrelid::regclass::text AS tbl
               FROM pg_constraint
              WHERE connamespace='public'::regnamespace AND contype='f' AND conname IN (
                'fk_activity_logs_user','fk_qpr_proposal','fk_qpr_created_by','fk_qpr_submitted_by',
                'fk_project_reports_proposal','fk_project_reports_submitted_by','fk_project_reports_reviewed_by',
                'fk_psr_reviewer','fk_proposals_proposal_announcement','fk_pau_actor',
                'fk_proposals_returned_by_user','fk_proposals_revision_forwarded_by_user','fk_ovcaf_released_by',
                'fk_dean_assignments_created_by','fk_dean_assignments_revoked_by')
    LOOP
        EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I', c.tbl, c.conname);
        RAISE NOTICE 'dropped %', c.conname;
    END LOOP;
END $rb01_fks$;

COMMIT;

SELECT 'after rollback: NORM-01 fks remaining (expected 0)' AS check_name, count(*)::text AS value
  FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype='f' AND conname IN (
    'fk_activity_logs_user','fk_qpr_proposal','fk_qpr_created_by','fk_qpr_submitted_by',
    'fk_project_reports_proposal','fk_project_reports_submitted_by','fk_project_reports_reviewed_by',
    'fk_psr_reviewer','fk_proposals_proposal_announcement','fk_pau_actor',
    'fk_proposals_returned_by_user','fk_proposals_revision_forwarded_by_user','fk_ovcaf_released_by',
    'fk_dean_assignments_created_by','fk_dean_assignments_revoked_by');
