-- =============================================================================
-- RPIS migration: align chk_notifications_type with the application vocabulary
-- =============================================================================
-- migration-01-db-audit-integrity.sql added chk_notifications_type as a NOT VALID
-- CHECK listing only the notification types that existed at that time:
--
--   PROPOSAL_UPDATE, REVIEW_REQUEST, APPROVAL, REVISION, REJECTION, FUNDING,
--   ENDORSEMENT, PROPOSAL_ANNOUNCEMENT
--
-- The application has since started writing additional types defined in
-- com.rpis.backend.util.NotificationTypes. A NOT VALID CHECK is still enforced for
-- every new or updated row, so each of those inserts failed with
--
--   new row for relation "notifications" violates check constraint
--   "chk_notifications_type"
--
-- which aborted the surrounding transaction. The RPS -&gt; OVCRIGE endorsement is one
-- of them (PROPOSAL_ENDORSED): its notification insert failed after the proposal
-- row had been updated, so PUT /api/proposals/{id}/endorse rolled back and answered
-- 409 Conflict. The other newer types (QUARTERLY_REPORT, PROJECT_COMPLETION,
-- DEAN_ASSIGNMENT, SPECIAL_ORDER_DRAFT, REVISION_DEADLINE_REMINDER) were blocked
-- the same way.
--
-- This migration replaces the constraint with the full canonical vocabulary:
--   * idempotent - it does nothing when the constraint already allows every type;
--   * data-preserving - no existing notification row is read, rewritten or deleted;
--   * NOT VALID, exactly like the constraint it replaces, so historical rows written
--     before the constraint existed are left untouched while every new row is
--     enforced;
--   * kept in step with the Java vocabulary by
--     NotificationTypeConstraintCoverageTest, so a type added to
--     com.rpis.backend.util.NotificationTypes cannot silently miss the database.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $widen_notification_type$
DECLARE
    required   text[] := ARRAY[
        'PROPOSAL_UPDATE',
        'REVIEW_REQUEST',
        'APPROVAL',
        'REVISION',
        'REJECTION',
        'FUNDING',
        'ENDORSEMENT',
        'PROPOSAL_ENDORSED',
        'PROPOSAL_SUBMISSION',
        'PROPOSAL_ANNOUNCEMENT',
        'QUARTERLY_REPORT',
        'PROJECT_COMPLETION',
        'DEAN_ASSIGNMENT',
        'SPECIAL_ORDER_DRAFT',
        'REVISION_DEADLINE_REMINDER',
        'EVALUATION_DEADLINE_REMINDER'
    ];
    definition text;
    value      text;
    missing    text := '';
BEGIN
    SELECT pg_get_constraintdef(c.oid)
      INTO definition
      FROM pg_constraint c
      JOIN pg_class     t ON t.oid = c.conrelid
      JOIN pg_namespace n ON n.oid = t.relnamespace
     WHERE c.conname = 'chk_notifications_type'
       AND n.nspname = 'public'
       AND t.relname = 'notifications';

    IF definition IS NOT NULL THEN
        FOREACH value IN ARRAY required LOOP
            IF position(quote_literal(value) IN definition) = 0 THEN
                missing := missing || ' ' || value;
            END IF;
        END LOOP;

        IF missing = '' THEN
            RAISE NOTICE 'chk_notifications_type already allows every notification type; nothing to do.';
            RETURN;
        END IF;

        EXECUTE 'ALTER TABLE public.notifications DROP CONSTRAINT chk_notifications_type';
        RAISE NOTICE 'chk_notifications_type was missing:%', missing;
    END IF;

    EXECUTE format(
        'ALTER TABLE public.notifications ADD CONSTRAINT chk_notifications_type '
        'CHECK (type IS NULL OR type IN (%s)) NOT VALID',
        array_to_string(ARRAY(SELECT quote_literal(t) FROM unnest(required) AS t), ', '));
    RAISE NOTICE 'chk_notifications_type now allows all % notification types (existing rows not validated).',
        array_length(required, 1);
END $widen_notification_type$;

COMMIT;

-- ===========================================================================
-- VERIFICATION (read-only)
-- ===========================================================================
SELECT 'chk_notifications_type present (expected 1)' AS check_name, count(*)::text AS value
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace
   AND conname = 'chk_notifications_type'
UNION ALL
SELECT 'all application notification types allowed (expected 16)',
       count(*)::text
  FROM pg_constraint c
 CROSS JOIN (VALUES
        ('PROPOSAL_UPDATE'), ('REVIEW_REQUEST'), ('APPROVAL'), ('REVISION'),
        ('REJECTION'), ('FUNDING'), ('ENDORSEMENT'), ('PROPOSAL_ENDORSED'),
        ('PROPOSAL_SUBMISSION'), ('PROPOSAL_ANNOUNCEMENT'), ('QUARTERLY_REPORT'),
        ('PROJECT_COMPLETION'), ('DEAN_ASSIGNMENT'), ('SPECIAL_ORDER_DRAFT'),
        ('REVISION_DEADLINE_REMINDER'), ('EVALUATION_DEADLINE_REMINDER')) AS t(code)
 WHERE c.conname = 'chk_notifications_type'
   AND c.connamespace = 'public'::regnamespace
   AND position(quote_literal(t.code) IN pg_get_constraintdef(c.oid)) > 0;
