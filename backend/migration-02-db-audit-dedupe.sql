-- ===========================================================================
-- RPIS database audit - Migration 02 of 03: missing relationship columns
-- ===========================================================================
-- HISTORICAL NOTE ON THIS FILENAME
--   An earlier plan gave this step the name "dedupe": it would have dropped the
--   dead application_cycles table, dropped the 7 unused legacy columns on
--   proposals and backfilled proposal_cooperating_agencies from the free-text
--   proposals.cooperating_agencies column. That plan was WITHDRAWN at your
--   instruction ("Do not drop tables or columns"), and the backfill was dropped
--   as impossible. This file therefore performs NO deduplication and removes
--   NOTHING. The filename is kept only because it was requested; the accurate
--   name would be migration-02-db-audit-relationship-columns.sql.
--
-- APPROVED SCOPE FOR THIS FILE (do not extend without approval)
--   2 nullable columns  |  2 foreign keys.  Nothing else.
--   NO BACKFILL. NO UPDATE. NO DELETE. This file writes 0 rows to existing data.
--   No column and no table is dropped.
--   No college/department foreign key. No agency backfill. No archive table.
--   notifications.proposal_id is NOT rewritten.
--   No foreign key for endorsement_request_events.actor_id.
--
--   notifications.endorsement_request_id  (nullable BIGINT, FK -> endorsement_requests)
--     notifications.proposal_id is polymorphic and type='ENDORSEMENT' is NOT a
--     reliable discriminator: EndorsementRequestService line 887 stores an
--     endorsement_requests id there, while ProposalService lines 1600 and 1639
--     store a proposals id under the same type. Measured on rpis_db: of 155
--     type='ENDORSEMENT' rows, 31 resolve to endorsement_requests, 76 resolve to
--     proposals and 46 resolve to neither (dangling). Ambiguous (id present in
--     both tables): 0. The column is therefore created NULL and LEFT NULL.
--     Going forward EndorsementRequestService writes it IN PARALLEL with
--     proposal_id, because EndorsementWorkflowPostgresTest:394 and
--     EndorsementRequestApiTest:295 assert proposalId == the endorsement request
--     id and src/utils/notificationTarget.js opens the endorsement from it. The
--     API contract is consequently unchanged.
--
--   proposals.rps_reviewer_id  (nullable BIGINT, FK -> users)
--     Backfill is impossible: of the 20 proposals with reviewed_by set, 0 have a
--     unique case-insensitive match on users.name, 19 match nobody, and 1 is
--     ambiguous (2 users, the first being a PROPONENT). users also holds 10
--     duplicated names. reviewed_by and reviewed_by_position are preserved
--     exactly as they are; the new column stays NULL until a human decides.
--
-- PRESERVED DECISIONS (verified intact in this file)
--   * endorsement_request_events ids 11 and 12 are NOT modified.
--   * No FK for endorsement_request_events.actor_id.
--   * No notifications.proposal_id foreign key is created.
--   * notifications.proposal_id is NOT rewritten: this file updates 0 rows.
--   * No college/department foreign key. Existing audit records preserved.
--   * No college, department or agency data is invented.
--
-- IDEMPOTENCY - all four states handled safely
--   Section 1 asserts that both columns exist BEFORE any query references them,
--   so a statement can never be sent against a column that is not there.
--   Combined with ADD COLUMN IF NOT EXISTS (Section 0) and pg_constraint-guarded
--   foreign keys (Section 3):
--     state 1  neither column exists (fresh run)
--                -> columns created, guards run (0 orphans), 2 FKs created
--     state 2  "column absent, an archive exists" - NOT APPLICABLE: this file
--                creates no archive and drops no column. A missing column is
--                re-created by ADD COLUMN IF NOT EXISTS before anything queries it.
--     state 3  partially applied (column present, FK missing)
--                -> preconditions pass, guard passes, only the missing FK is created
--     state 4  already complete
--                -> preconditions pass, ADD COLUMN skipped, guards pass, both FKs
--                   found by name -> no DDL executed
--   Safe to re-run before or after 01 and 03: the verification block counts only
--   this file's own two columns and two foreign keys and never asserts a global
--   schema total.
--
-- TRANSACTION AND FAILURE BEHAVIOUR
--   One explicit transaction. ON_ERROR_STOP=1 is MANDATORY: a RAISE EXCEPTION
--   aborts the transaction, psql stops without running the rest, exits non-zero,
--   and the server rolls back the open transaction when the connection closes.
--   Nothing is partially applied. Verification SELECTs run after COMMIT.
--
-- Preconditions verified against the rpis_db snapshot of 2026-11-08.
--
-- Apply with:
--   psql -v ON_ERROR_STOP=1 -h localhost -U postgres -d <database> \
--        -f migration-02-db-audit-dedupe.sql
-- ===========================================================================

\set ON_ERROR_STOP on
BEGIN;

-- ===========================================================================
-- SECTION 0 - COLUMN CREATION (idempotent; creates nothing if already present)
-- ===========================================================================
ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS endorsement_request_id BIGINT;

ALTER TABLE public.proposals
    ADD COLUMN IF NOT EXISTS rps_reviewer_id BIGINT;
-- ===========================================================================
-- SECTION 1 - PRECONDITIONS
-- ---------------------------------------------------------------------------
-- Runs AFTER Section 0, so both columns are guaranteed to exist. If either were
-- somehow absent (a dropped column, a partially applied run, a mistyped table),
-- this block names it and aborts BEFORE any statement queries it.
-- ===========================================================================
DO $m02_pre$
DECLARE
    ref     record;
    missing text := '';
BEGIN
    FOR ref IN
        SELECT * FROM (VALUES
            ('notifications',        'endorsement_request_id'),
            ('proposals',            'rps_reviewer_id'),
            ('endorsement_requests', 'id'),
            ('users',                'id')
        ) AS t(tbl, col)
    LOOP
        IF to_regclass(format('public.%I', ref.tbl)) IS NULL THEN
            missing := missing || format(E'\n  - missing TABLE public.%s', ref.tbl);
        ELSIF NOT EXISTS (
            SELECT 1 FROM pg_attribute a
              JOIN pg_class     c ON c.oid = a.attrelid
              JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE n.nspname = 'public' AND c.relname = ref.tbl
               AND a.attname = ref.col AND a.attnum > 0 AND NOT a.attisdropped)
        THEN
            missing := missing || format(E'\n  - missing COLUMN public.%s.%s', ref.tbl, ref.col);
        END IF;
    END LOOP;

    IF missing <> '' THEN
        RAISE EXCEPTION 'PRECONDITION FAILED - cannot proceed:%', missing;
    END IF;
    RAISE NOTICE 'M02 Section 1 preconditions OK (both columns present).';
END $m02_pre$;

-- ===========================================================================
-- SECTION 2 - ORPHAN GUARDS (fatal; both pass on the audited snapshot)
-- ---------------------------------------------------------------------------
-- Both columns are created NULL and left NULL by this file, so these guards can
-- only fail on a re-run against data written in the meantime. NULL is not an
-- orphan reference (MATCH SIMPLE semantics), so both guards filter "IS NOT NULL"
-- exactly as the foreign keys they protect do.
-- ===========================================================================
DO $m02_guards$
DECLARE
    n        bigint;
    sample   text;
    failures text := '';
BEGIN
    EXECUTE 'SELECT count(*) FROM public.notifications c '
            'WHERE c.endorsement_request_id IS NOT NULL '
            'AND NOT EXISTS (SELECT 1 FROM public.endorsement_requests p '
            '                 WHERE p.id = c.endorsement_request_id)'
    INTO n;
    IF n > 0 THEN
        EXECUTE 'SELECT string_agg(s.id::text, '', '' ORDER BY s.id) FROM ('
                '  SELECT c.id FROM public.notifications c '
                '   WHERE c.endorsement_request_id IS NOT NULL '
                '   AND NOT EXISTS (SELECT 1 FROM public.endorsement_requests p '
                '                    WHERE p.id = c.endorsement_request_id) '
                '   ORDER BY c.id LIMIT 20) s'
        INTO sample;
        failures := failures || format(
            E'\n  - notifications.endorsement_request_id -> endorsement_requests(id): % orphan row(s); offending id(s) (max 20): %',
            n, coalesce(sample, '(none)'));
    ELSE
        RAISE NOTICE 'FATAL guard OK: notifications.endorsement_request_id -> endorsement_requests(id)';
    END IF;

    EXECUTE 'SELECT count(*) FROM public.proposals c '
            'WHERE c.rps_reviewer_id IS NOT NULL '
            'AND NOT EXISTS (SELECT 1 FROM public.users p WHERE p.id = c.rps_reviewer_id)'
    INTO n;
    IF n > 0 THEN
        EXECUTE 'SELECT string_agg(s.id::text, '', '' ORDER BY s.id) FROM ('
                '  SELECT c.id FROM public.proposals c '
                '   WHERE c.rps_reviewer_id IS NOT NULL '
                '   AND NOT EXISTS (SELECT 1 FROM public.users p WHERE p.id = c.rps_reviewer_id) '
                '   ORDER BY c.id LIMIT 20) s'
        INTO sample;
        failures := failures || format(
            E'\n  - proposals.rps_reviewer_id -> users(id): % orphan row(s); offending id(s) (max 20): %',
            n, coalesce(sample, '(none)'));
    ELSE
        RAISE NOTICE 'FATAL guard OK: proposals.rps_reviewer_id -> users(id)';
    END IF;

    IF failures <> '' THEN
        RAISE EXCEPTION
            'FATAL ORPHAN GUARDS FAILED. No constraint was created and no row was modified:%', failures;
    END IF;
END $m02_guards$;

-- ===========================================================================
-- SECTION 3 - FOREIGN KEYS (2 constraints)
-- ---------------------------------------------------------------------------
-- Fully validated, not NOT VALID, which is safe because the guards above abort
-- first. Names checked against the 33 existing foreign keys: no collision.
-- Both columns stay plain Long in the JPA entities and must NOT become
-- @ManyToOne, or the dev profile would add a duplicate foreign key.
-- ===========================================================================
DO $m02_fks$
DECLARE
    entry record;
BEGIN
    FOR entry IN
        SELECT * FROM (VALUES
            ('fk_notifications_endorsement_request', 'notifications', 'endorsement_request_id', 'endorsement_requests', 'SET NULL'),
            ('fk_proposals_rps_reviewer',            'proposals',     'rps_reviewer_id',        'users',                'SET NULL')
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
END $m02_fks$;
COMMIT;

-- ===========================================================================
-- VERIFICATION - this file's own objects only.
-- ---------------------------------------------------------------------------
-- IMPORTANT - HOW TO READ THIS OUTPUT
--   This migration ADDS NULLABLE COLUMNS AND FOREIGN KEYS ONLY. It performs NO
--   backfill and NO UPDATE: it writes 0 rows to existing data. The two rows
--   labelled "INFO" count rows where the APPLICATION has since populated the new
--   columns. A non-zero value there is the application working correctly and is
--   NOT a failure of this migration. Only the rows labelled ASSERTION are
--   assertions.
-- ===========================================================================
SELECT 'M02 ASSERTION - columns present (expected 2)' AS check_name, count(*)::text AS value
  FROM information_schema.columns
 WHERE table_schema = 'public'
   AND ((table_name = 'notifications' AND column_name = 'endorsement_request_id')
     OR (table_name = 'proposals'     AND column_name = 'rps_reviewer_id'))
UNION ALL
SELECT 'M02 ASSERTION - foreign keys present (expected 2)', count(*)::text
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace AND contype = 'f'
   AND conname IN ('fk_notifications_endorsement_request','fk_proposals_rps_reviewer')
UNION ALL
SELECT 'INFO - proposals rows with rps_reviewer_id set (this migration wrote 0; the application populates it later)',
       count(*)::text
  FROM public.proposals WHERE rps_reviewer_id IS NOT NULL
UNION ALL
SELECT 'INFO - notifications rows with endorsement_request_id set (this migration wrote 0; the application populates it later)',
       count(*)::text
  FROM public.notifications WHERE endorsement_request_id IS NOT NULL
UNION ALL
SELECT 'INFO - notifications rows with proposal_id set (this migration performs no UPDATE, so this matches the pre-migration value)',
       count(*)::text
  FROM public.notifications WHERE proposal_id IS NOT NULL
UNION ALL
SELECT 'M02 ASSERTION - indexes covering either new column (expected 0: adding a foreign key creates no index in PostgreSQL)',
       count(*)::text
  FROM pg_index i
  JOIN pg_class     c ON c.oid = i.indrelid
  JOIN pg_namespace n ON n.oid = c.relnamespace
  JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
 WHERE n.nspname = 'public'
   AND ((c.relname = 'notifications' AND a.attname = 'endorsement_request_id')
     OR (c.relname = 'proposals'     AND a.attname = 'rps_reviewer_id'));
