-- =============================================================================
-- RPIS migration: align the activity_logs CHECK constraints with the application
-- vocabulary
-- =============================================================================
-- migration-01-db-audit-integrity.sql added two NOT VALID CHECK constraints on
-- activity_logs, listing only the vocabulary that existed then:
--
--   chk_activity_logs_action: ACCOUNT_CREATED, ACCOUNT_UPDATED, ACCOUNT_APPROVED,
--                             ACCOUNT_REJECTED, ROLE_CHANGED, PASSWORD_CHANGED,
--                             PROFILE_UPDATED, CALL_CREATED, CALL_UPDATED,
--                             CALL_OPENED, CALL_CLOSED, SETTINGS_UPDATED
--   chk_activity_logs_module: 'User Accounts', 'Profile', 'Calls for Proposal',
--                             'System Settings'
--
-- The Activity Log now records every RPS Administration module. The Dean and
-- Priority Agenda actions were added to
-- com.rpis.backend.model.ActivityLog, but a NOT VALID CHECK still applies to
-- every new row, so each such insert failed with
--
--   new row for relation "activity_logs" violates check constraint
--   "chk_activity_logs_action"
--
-- which marked the surrounding administrative transaction rollback-only.
--
-- This migration replaces both constraints with the full canonical vocabulary.
-- It is idempotent, data-preserving (no row is read, rewritten or deleted) and
-- keeps both constraints NOT VALID exactly like the ones they replace, so
-- historical rows written before the constraint existed are left untouched while
-- every new row is enforced.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $widen_activity_log_action$
DECLARE
    required   text[] := ARRAY[
        'ACCOUNT_CREATED',
        'ACCOUNT_UPDATED',
        'ACCOUNT_APPROVED',
        'ACCOUNT_REJECTED',
        'ROLE_CHANGED',
        'PASSWORD_CHANGED',
        'PASSWORD_RESET',
        'PROFILE_UPDATED',
        'CALL_CREATED',
        'CALL_UPDATED',
        'CALL_OPENED',
        'CALL_CLOSED',
        'CALL_DELETED',
        'SETTINGS_UPDATED',
        'DEAN_ASSIGNED',
        'DEAN_REPLACED',
        'DEAN_REVOKED',
        'FRAMEWORK_CREATED',
        'FRAMEWORK_UPDATED',
        'FRAMEWORK_ACTIVATED',
        'FRAMEWORK_DEACTIVATED',
        'AGENDA_CREATED',
        'AGENDA_UPDATED',
        'AGENDA_ACTIVATED',
        'AGENDA_DEACTIVATED'
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
     WHERE c.conname = 'chk_activity_logs_action'
       AND n.nspname = 'public'
       AND t.relname = 'activity_logs';

    IF definition IS NOT NULL THEN
        FOREACH value IN ARRAY required LOOP
            IF position(quote_literal(value) IN definition) = 0 THEN
                missing := missing || ' ' || value;
            END IF;
        END LOOP;

        IF missing = '' THEN
            RAISE NOTICE 'chk_activity_logs_action already allows every action; nothing to do.';
            RETURN;
        END IF;

        EXECUTE 'ALTER TABLE public.activity_logs DROP CONSTRAINT chk_activity_logs_action';
        RAISE NOTICE 'chk_activity_logs_action was missing:%', missing;
    END IF;

    EXECUTE format(
        'ALTER TABLE public.activity_logs ADD CONSTRAINT chk_activity_logs_action '
        'CHECK (action IN (%s)) NOT VALID',
        array_to_string(ARRAY(SELECT quote_literal(t) FROM unnest(required) AS t), ', '));
    RAISE NOTICE 'chk_activity_logs_action now allows all % actions (existing rows not validated).',
        array_length(required, 1);
END $widen_activity_log_action$;

DO $widen_activity_log_module$
DECLARE
    required   text[] := ARRAY[
        'User Accounts',
        'Dean Role Management',
        'Priority Agenda',
        'Profile',
        'Calls for Proposal',
        'System Settings'
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
     WHERE c.conname = 'chk_activity_logs_module'
       AND n.nspname = 'public'
       AND t.relname = 'activity_logs';

    IF definition IS NOT NULL THEN
        FOREACH value IN ARRAY required LOOP
            IF position(quote_literal(value) IN definition) = 0 THEN
                missing := missing || ' ' || value;
            END IF;
        END LOOP;

        IF missing = '' THEN
            RAISE NOTICE 'chk_activity_logs_module already allows every module; nothing to do.';
            RETURN;
        END IF;

        EXECUTE 'ALTER TABLE public.activity_logs DROP CONSTRAINT chk_activity_logs_module';
        RAISE NOTICE 'chk_activity_logs_module was missing:%', missing;
    END IF;

    EXECUTE format(
        'ALTER TABLE public.activity_logs ADD CONSTRAINT chk_activity_logs_module '
        'CHECK (module IN (%s)) NOT VALID',
        array_to_string(ARRAY(SELECT quote_literal(t) FROM unnest(required) AS t), ', '));
    RAISE NOTICE 'chk_activity_logs_module now allows all % modules (existing rows not validated).',
        array_length(required, 1);
END $widen_activity_log_module$;

COMMIT;

-- ===========================================================================
-- VERIFICATION (read-only)
-- ===========================================================================
SELECT 'chk_activity_logs_action present (expected 1)' AS check_name, count(*)::text AS value
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace
   AND conname = 'chk_activity_logs_action'
UNION ALL
SELECT 'chk_activity_logs_module present (expected 1)', count(*)::text
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace
   AND conname = 'chk_activity_logs_module'
UNION ALL
SELECT 'all application activity actions allowed (expected 25)', count(*)::text
  FROM pg_constraint c
 CROSS JOIN (VALUES
        ('ACCOUNT_CREATED'), ('ACCOUNT_UPDATED'), ('ACCOUNT_APPROVED'),
        ('ACCOUNT_REJECTED'), ('ROLE_CHANGED'), ('PASSWORD_CHANGED'),
        ('PASSWORD_RESET'), ('PROFILE_UPDATED'), ('CALL_CREATED'),
        ('CALL_UPDATED'), ('CALL_OPENED'), ('CALL_CLOSED'), ('CALL_DELETED'),
        ('SETTINGS_UPDATED'), ('DEAN_ASSIGNED'), ('DEAN_REPLACED'),
        ('DEAN_REVOKED'), ('FRAMEWORK_CREATED'), ('FRAMEWORK_UPDATED'),
        ('FRAMEWORK_ACTIVATED'), ('FRAMEWORK_DEACTIVATED'), ('AGENDA_CREATED'),
        ('AGENDA_UPDATED'), ('AGENDA_ACTIVATED'), ('AGENDA_DEACTIVATED')) AS t(code)
 WHERE c.conname = 'chk_activity_logs_action'
   AND c.connamespace = 'public'::regnamespace
   AND position(quote_literal(t.code) IN pg_get_constraintdef(c.oid)) > 0
UNION ALL
SELECT 'all application activity modules allowed (expected 6)', count(*)::text
  FROM pg_constraint c
 CROSS JOIN (VALUES
        ('User Accounts'), ('Dean Role Management'), ('Priority Agenda'),
        ('Profile'), ('Calls for Proposal'), ('System Settings')) AS t(name)
 WHERE c.conname = 'chk_activity_logs_module'
   AND c.connamespace = 'public'::regnamespace
   AND position(quote_literal(t.name) IN pg_get_constraintdef(c.oid)) > 0;
