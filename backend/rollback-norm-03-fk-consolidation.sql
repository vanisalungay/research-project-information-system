-- =============================================================================
-- Rollback for NORM-03 (foreign-key consolidation) - rpis_db
-- =============================================================================
-- Removes the one object NORM-03 ADDS:
--   notifications.proposal_announcement_id -> proposal_announcements(id)
--
-- It deliberately does NOT recreate the duplicate foreign keys NORM-03 removed
-- (fk_qpr_proposal's anonymous twin on quarterly_progress_reports.proposal_id,
-- and the duplicate created_by / submitted_by constraints). Those duplicates
-- were a defect: they enforced the same relationship twice and the
-- proposal_id twin disagreed on the delete action (CASCADE vs RESTRICT).
-- To recover the exact pre-NORM-03 catalog, restore the pre-migration backup
-- instead of adding redundant constraints back.
--
-- The rename convergence (an anonymous <table>_<column>_fkey that NORM-03
-- renamed to the agreed name) is likewise not undone: the relationship and its
-- delete action are unchanged by a rename.
--
-- No table, column or row is touched. Safe to run repeatedly.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $rb03$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
                WHERE conname = 'fk_notifications_proposal_announcement'
                  AND connamespace = 'public'::regnamespace) THEN
        ALTER TABLE public.notifications
            DROP CONSTRAINT fk_notifications_proposal_announcement;
        RAISE NOTICE 'dropped fk_notifications_proposal_announcement';
    END IF;
END $rb03$;

COMMIT;

SELECT 'after rollback: notifications.proposal_announcement_id FK remaining (expected 0)' AS check_name,
       count(*)::text AS value
  FROM pg_constraint c
  JOIN pg_class     t  ON t.oid = c.conrelid
  JOIN pg_namespace ns ON ns.oid = t.relnamespace
  JOIN pg_attribute a  ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
 WHERE c.contype = 'f'
   AND ns.nspname = 'public'
   AND t.relname  = 'notifications'
   AND a.attname  = 'proposal_announcement_id';
