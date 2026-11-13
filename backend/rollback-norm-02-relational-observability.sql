-- =============================================================================
-- Rollback for NORM-02 (referential-defect observability views)
-- =============================================================================
-- Drops ONLY the five read-only views created by
-- migration-norm-02-relational-observability.sql.
-- No table, column, constraint or row is touched by this file.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;
DROP VIEW IF EXISTS public.v_referential_integrity_summary;
DROP VIEW IF EXISTS public.v_unmatched_proposal_priority_agendas;
DROP VIEW IF EXISTS public.v_orphan_endorsement_request_events_actor;
DROP VIEW IF EXISTS public.v_orphan_notifications_proposal_announcement;
DROP VIEW IF EXISTS public.v_orphan_notifications_proposal;
COMMIT;
SELECT 'after rollback: NORM-02 views remaining (expected 0)' AS check_name, count(*)::text AS value
  FROM pg_views WHERE schemaname='public' AND viewname LIKE 'v_%';
