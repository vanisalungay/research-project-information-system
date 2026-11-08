-- ===========================================================================
-- RPIS database audit - ROLLBACK of migration 03 only
-- ===========================================================================
-- Reverses migration-03-db-audit-status-history.sql. It does NOT touch
-- migrations 01 or 02.
--
-- THE ROLLBACK CANNOT DESTROY APPLICATION-RECORDED HISTORY
--   Two independent safety layers, both preserved from the previous revision:
--     layer 1  If proposal_status_history contains ANY row with
--              source IS DISTINCT FROM BASELINE_SEED - that is, any row a
--              running application wrote - the rollback REFUSES to proceed and
--              raises an exception, unless approval was explicitly passed.
--     layer 2  Even when approval IS given, only rows with
--              source = BASELINE_SEED are deleted, and the non-baseline row
--              count is asserted to be unchanged before COMMIT. If any
--              non-baseline row survives, the table is KEPT, never dropped.
--   proposal_statuses is dropped only when it contains no code outside the 28
--   seeded values, so an administrator-added status code is never silently lost.
--
-- DEFAULT BEHAVIOUR: REFUSE
--   psql -v ON_ERROR_STOP=1 -h localhost -U postgres -d <database> \
--        -f rollback-03-db-audit-status-history.sql
--
-- EXPLICITLY APPROVED ROLLBACK
--   psql -v ON_ERROR_STOP=1 -v rollback_approved=true \
--        -h localhost -U postgres -d <database> \
--        -f rollback-03-db-audit-status-history.sql
--
-- HOW THE psql VARIABLE IS PASSED
--   The approval value is NOT interpolated inside the DO block. It is
--   interpolated in a plain SQL statement OUTSIDE any dollar-quoted string and
--   stored as a session setting, which the DO block then reads with
--   current_setting(). The script therefore does not depend on psql variable
--   interpolation rules inside dollar-quoted strings at all.
--
--   The comparison is deliberately forgiving: true, t, 1, yes and on (any case)
--   all count as approved.
--
-- TABLES ARE CHECKED BEFORE THEIR CONTENTS ARE READ
--   to_regclass() is tested first, and every statement that touches a table is
--   issued through EXECUTE dynamic SQL with parameters passed via USING. A missing
--   table therefore produces a clean NOTICE instead of a raw
--   "relation does not exist" error, and no string-literal quoting is required.
--
-- CONDITIONAL VERIFICATION (fix for the previous revision)
--   The previous revision asserted that both tables would be absent. That was
--   wrong: the rollback INTENTIONALLY RETAINS proposal_status_history when
--   application-recorded rows remain, and INTENTIONALLY RETAINS
--   proposal_statuses when administrator-added codes exist. Verification is now
--   conditional and reports the actual state plus the reason, so a legitimate
--   retention is never reported as a failure.
--
-- IDEMPOTENCY
--   Safe to run repeatedly. Once both tables are gone it prints "nothing to do"
--   and changes nothing.
--
-- ON_ERROR_STOP=1 is MANDATORY: an exception aborts the transaction, psql stops
-- and exits non-zero, and the server rolls back the open transaction when the
-- connection closes. Nothing is partially applied.
-- ===========================================================================

\set ON_ERROR_STOP on

-- Read the approval flag without overwriting a value supplied with -v.
\if :{?rollback_approved}
\else
    \set rollback_approved false
\endif

-- Interpolated here, OUTSIDE any dollar-quoted string, then read inside the DO block.
SELECT set_config('rpis.rollback_approved', :'rollback_approved', false) AS rollback_approved;

BEGIN;
DO $rb_action$
DECLARE
    approved        boolean := lower(coalesce(current_setting('rpis.rollback_approved', true), 'false'))
                               IN ('true','t','1','yes','on');
    hist_exists     boolean;
    ref_exists      boolean;
    total_rows      bigint := 0;
    baseline_rows   bigint := 0;
    app_rows_before bigint := 0;
    app_rows_after  bigint := 0;
    unknown_codes   bigint := 0;
BEGIN
    hist_exists := to_regclass('public.proposal_status_history') IS NOT NULL;
    ref_exists  := to_regclass('public.proposal_statuses')       IS NOT NULL;

    IF NOT hist_exists AND NOT ref_exists THEN
        RAISE NOTICE 'Rollback 03: neither table exists - nothing to do.';
        RETURN;
    END IF;

    IF hist_exists THEN
        EXECUTE 'SELECT count(*) FROM public.proposal_status_history'
           INTO total_rows;
        EXECUTE 'SELECT count(*) FROM public.proposal_status_history WHERE source = $1'
           INTO baseline_rows USING 'BASELINE_SEED';
        EXECUTE 'SELECT count(*) FROM public.proposal_status_history WHERE source IS DISTINCT FROM $1'
           INTO app_rows_before USING 'BASELINE_SEED';

        RAISE NOTICE 'Rollback 03: proposal_status_history - total=%, baseline=%, application-recorded=%',
                     total_rows, baseline_rows, app_rows_before;
    END IF;

    -- LAYER 1: refuse to remove real history unless explicitly approved.
    IF app_rows_before > 0 AND NOT approved THEN
        RAISE EXCEPTION
            'ROLLBACK REFUSED. public.proposal_status_history contains % row(s) recorded by the '
            'application (source <> BASELINE_SEED). Dropping the table would destroy real status '
            'history, so nothing was changed. Re-run with -v rollback_approved=true ONLY if that loss '
            'is intended.', app_rows_before;
    END IF;

    IF hist_exists THEN
        -- LAYER 2: remove only what migration 03 itself inserted.
        EXECUTE 'DELETE FROM public.proposal_status_history WHERE source = $1' USING 'BASELINE_SEED';

        EXECUTE 'SELECT count(*) FROM public.proposal_status_history WHERE source IS DISTINCT FROM $1'
           INTO app_rows_after USING 'BASELINE_SEED';

        IF app_rows_after <> app_rows_before THEN
            RAISE EXCEPTION
                'ROLLBACK SAFETY VIOLATION: application-recorded rows changed from % to %. '
                'The transaction is aborted and nothing was committed.', app_rows_before, app_rows_after;
        END IF;

        IF app_rows_after = 0 THEN
            EXECUTE 'DROP TABLE public.proposal_status_history';
            RAISE NOTICE 'Rollback 03: proposal_status_history DROPPED (it held only BASELINE_SEED rows).';
        ELSE
            RAISE NOTICE
                'Rollback 03: % application-recorded row(s) were PRESERVED and the table was KEPT.',
                app_rows_after;
        END IF;
    END IF;

    IF ref_exists THEN
        -- Pass the 28 seeded codes as an array parameter, which avoids building a
        -- long quoted literal inside dynamic SQL.
        EXECUTE 'SELECT count(*) FROM public.proposal_statuses WHERE code <> ALL ($1)'
           INTO unknown_codes
          USING ARRAY['DRAFT','SUBMITTED','RPS_RETURNED','RETURNED_TO_RPS','ENDORSED',
                      'OVCRIGE_COORDINATION','UNDER_REVIEW','REC_EVALUATION','REC_APPROVED',
                      'OVC_APPROVED','CHANCELLOR_REVIEW','FOR_OC_APPROVAL','FOR_OVCAF_APPROVAL',
                      'FINANCE_AUTHORIZATION','READY_FOR_RELEASE','RELEASED','IMPLEMENTATION',
                      'APPROVED','APPROVED_ENDORSED','COMPLETED','REJECTED','REC_REJECTED',
                      'REVISION','REC_REVISION','PENDING_REVISION','RETURNED',
                      'RETURNED_FOR_REVISION','RPS_REVIEW']::text[];

        IF unknown_codes = 0 THEN
            EXECUTE 'DROP TABLE public.proposal_statuses';
            RAISE NOTICE 'Rollback 03: proposal_statuses DROPPED (reference data only).';
        ELSE
            RAISE NOTICE
                'Rollback 03: proposal_statuses KEPT - it holds % administrator-added status code(s).',
                unknown_codes;
        END IF;
    END IF;
END $rb_action$;
COMMIT;

DO $rb_report$
DECLARE
    hist_present boolean := to_regclass('public.proposal_status_history') IS NOT NULL;
    ref_present  boolean := to_regclass('public.proposal_statuses')       IS NOT NULL;
    hist_rows    bigint := 0;
    app_rows     bigint := 0;
    ref_rows     bigint := 0;
BEGIN
    IF hist_present THEN
        EXECUTE 'SELECT count(*) FROM public.proposal_status_history' INTO hist_rows;
        EXECUTE 'SELECT count(*) FROM public.proposal_status_history WHERE source IS DISTINCT FROM $1'
           INTO app_rows USING 'BASELINE_SEED';
    END IF;
    IF ref_present THEN
        EXECUTE 'SELECT count(*) FROM public.proposal_statuses' INTO ref_rows;
    END IF;

    RAISE NOTICE '--------------- ROLLBACK 03 REPORT ---------------';
    RAISE NOTICE 'proposal_status_history : %',
        CASE WHEN hist_present
             THEN format('PRESENT (%s row(s), of which %s application-recorded) - RETAINED BY DESIGN, real history preserved',
                         hist_rows, app_rows)
             ELSE 'ABSENT - dropped, because it held only BASELINE_SEED rows' END;
    RAISE NOTICE 'proposal_statuses       : %',
        CASE WHEN ref_present
             THEN format('PRESENT (%s row(s)) - RETAINED BY DESIGN, administrator-added codes present', ref_rows)
             ELSE 'ABSENT - dropped, because it held only the 28 seeded reference rows' END;
    RAISE NOTICE 'M03 FOREIGN KEYS left   : % (2 when the table was retained, 0 when it was dropped)',
        (SELECT count(*) FROM pg_constraint
          WHERE connamespace='public'::regnamespace AND contype='f'
            AND conname IN ('fk_psh_proposal','fk_psh_actor_user'));
    RAISE NOTICE 'M03 INDEX ENTRIES left  : % (3 when retained, 0 when dropped)',
        (SELECT count(*) FROM pg_indexes
          WHERE schemaname='public'
            AND indexname IN ('idx_psh_proposal_changed','proposal_statuses_pkey','proposal_status_history_pkey'));
    RAISE NOTICE 'Every scenario above is a valid rollback outcome.';
    RAISE NOTICE '-------------------------------------------------';
END $rb_report$;

-- ===========================================================================
-- INVARIANTS THAT MUST HOLD WHATEVER HAPPENED ABOVE
-- These read only tables that always exist, so they are safe unconditionally.
-- ===========================================================================
SELECT 'proposals rows (must be unchanged)' AS check_name, count(*)::text AS value
  FROM public.proposals
UNION ALL
SELECT 'notifications rows (must be unchanged)', count(*)::text
  FROM public.notifications
UNION ALL
SELECT 'endorsement_request_events rows (must be unchanged at 54)', count(*)::text
  FROM public.endorsement_request_events
UNION ALL
SELECT 'endorsement_request_events rows with actor_id = 79 (must still be 2 - never nulled)',
       count(*)::text
  FROM public.endorsement_request_events WHERE actor_id = 79
UNION ALL
SELECT 'endorsement_request_events ids 11 and 12 still present (must be 2)',
       count(*)::text
  FROM public.endorsement_request_events WHERE id IN (11, 12)
UNION ALL
SELECT 'proposals with a status (baseline source, must be unchanged at 26)',
       count(*)::text
  FROM public.proposals WHERE status IS NOT NULL;
