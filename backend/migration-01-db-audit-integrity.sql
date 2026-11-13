-- ===========================================================================
-- RPIS database audit - Migration 01 of 03: referential integrity
-- ===========================================================================
-- Adds the foreign keys, indexes and CHECK constraints the live rpis_db schema
-- is missing. ADDITIVE ONLY: no table, column or row is created, dropped,
-- renamed or rewritten, and no existing constraint is altered.
--
-- APPROVED SCOPE FOR THIS FILE (do not extend without approval)
--   16 foreign keys  |  2 indexes  |  15 CHECK constraints
--   Nothing is dropped. 0 rows are updated. 0 rows are deleted.
--   No foreign key is created for endorsement_request_events.actor_id.
--   notifications.proposal_id is not modified.
--
-- PRESERVED DECISIONS (verified intact in this file)
--   * endorsement_request_events ids 11 and 12 are NOT modified, and the FK for
--     endorsement_request_events.actor_id is NOT created.
--   * No notifications.proposal_id foreign key is created.
--   * notifications.proposal_id is NOT rewritten.
--   * No college/department foreign key appears anywhere.
--   * Existing audit records are preserved.
--   * No college, department or agency data is invented.
--   * No archive table, no agency backfill, no legacy-column cleanup.
--
-- IDEMPOTENCY
--   PostgreSQL has no "ADD CONSTRAINT IF NOT EXISTS" (confirmed against the
--   PostgreSQL 18 ALTER TABLE synopsis: "ADD table_constraint [ NOT VALID ]").
--   Every constraint is therefore created inside a DO block that looks the
--   constraint up in pg_constraint first. Indexes use
--   CREATE INDEX IF NOT EXISTS. Safe to re-run before or after 02 and 03: the
--   verification block counts ONLY THIS FILE'S named objects and never asserts a
--   global schema total.
--
-- DOLLAR-QUOTE TAGS (fix for the previous revision)
--   The Section 4 block previously used DO $$ ... $$ while the CHECK expressions
--   inside it were also written with $$. PostgreSQL cannot nest dollar-quoted
--   strings that share the same tag, so the outer string terminated at the first
--   inner $$ and the file did not parse. Every block now uses a UNIQUE tag.
--   Note that inside $m01_checks$ ... $m01_checks$ an inner $$ is legal, because
--   the lexer searches only for the matching $m01_checks$ tag.
--
-- TRANSACTION AND FAILURE BEHAVIOUR (matches PostgreSQL execution)
--   All statements run in ONE explicit transaction between BEGIN and COMMIT.
--   * ON_ERROR_STOP=1 is MANDATORY, not advisory. A RAISE EXCEPTION aborts the
--     transaction; psql then stops, does not run the remaining commands, exits
--     non-zero, and the server rolls back the open transaction when the
--     connection closes. Nothing is partially applied.
--   * A RAISE WARNING (used by the non-fatal diagnostic in Section 1B) does NOT
--     abort the transaction and does NOT stop the script.
--   * Without ON_ERROR_STOP psql would continue inside an aborted transaction,
--     printing "current transaction is aborted" for every later statement.
--   * The verification SELECTs sit AFTER COMMIT, so they read committed state.
--
--   CHECK constraints are added NOT VALID: the initial check of existing rows is
--   skipped, so the statement cannot fail on data, while the constraint IS still
--   enforced for subsequent INSERT and UPDATE. Until VALIDATE CONSTRAINT is run
--   the constraint remains not-validated (pg_constraint.convalidated = false) and
--   the planner will not use it as a proof. VALIDATE is deliberately NOT run
--   here; it is a separate, explicitly approved step.
--
--   FOREIGN KEYS are added FULLY VALIDATED, not NOT VALID, which is safe because
--   the Section 1A guards run first inside the same transaction and abort if any
--   child row is unreferenced. Honest consequence: a validated ADD FOREIGN KEY
--   scans the child table and holds a lock that blocks concurrent writes for its
--   duration. The largest child here is notifications (1,047 rows), so this is
--   instantaneous. For a large production table the documented pattern is
--   ADD ... NOT VALID followed by VALIDATE CONSTRAINT; that is intentionally NOT
--   the default here.
--
--   Indexes use plain CREATE INDEX: CREATE INDEX CONCURRENTLY cannot run inside a
--   transaction block, which is why the locking form is used deliberately.
--
-- Preconditions verified against the rpis_db snapshot of 2026-11-08:
--   29 base tables, 30 sequences, 29 PK, 33 FK, 10 UNIQUE, 4 CHECK,
--   77 indexes, 26 proposals (all with a status), 27 users,
--   1047 notifications, 16 quarterly reports.
--
-- Apply with:
--   psql -v ON_ERROR_STOP=1 -h localhost -U postgres -d <database> \
--        -f migration-01-db-audit-integrity.sql
-- ===========================================================================
\set ON_ERROR_STOP on
BEGIN;

-- ===========================================================================
-- SECTION 0 - PRECONDITIONS
-- ---------------------------------------------------------------------------
-- Fails with an explicit list instead of a cryptic runtime error if the schema
-- does not match the audited snapshot, and PROVES that each parent table has a
-- single-column primary key named "id" (which the guards below rely on). All 29
-- tables in rpis_db were verified to have such a key; this assertion makes the
-- guard's "p.id" a proven fact rather than an assumption.
-- ===========================================================================
DO $m01_pre$
DECLARE
    ref     record;
    missing text := '';
    bad_pk  text := '';
BEGIN
    FOR ref IN
        SELECT * FROM (VALUES
            ('proposals',                     'proposal_announcement_id'),
            ('proposals',                     'returned_by_user_id'),
            ('proposals',                     'revision_forwarded_by_user_id'),
            ('quarterly_progress_reports',    'proposal_id'),
            ('quarterly_progress_reports',    'created_by'),
            ('quarterly_progress_reports',    'submitted_by'),
            ('quarterly_progress_reports',    'copied_from_report_id'),
            ('project_reports',               'proposal_id'),
            ('project_reports',               'submitted_by'),
            ('project_reports',               'reviewed_by'),
            ('activity_logs',                 'user_id'),
            ('proposal_section_remarks',      'reviewer_id'),
            ('ovcaf_validations',             'released_by'),
            ('dean_assignments',              'created_by'),
            ('dean_assignments',              'revoked_by'),
            ('proposal_announcement_updates', 'actor_id'),
            ('endorsement_request_events',    'actor_id'),
            ('proposal_announcements',        'id'),
            ('proposals',                     'id'),
            ('users',                         'id'),
            ('quarterly_progress_reports',    'id'),
            ('endorsement_requests',          'id')
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

    -- every parent table must expose a single-column primary key named "id".
    -- pg_index.indkey is an int2vector and is 0-based, so indkey[0] is column 1.
    SELECT string_agg(pt.t, ', ') INTO bad_pk
      FROM (VALUES ('proposal_announcements'), ('proposals'), ('users'),
                   ('quarterly_progress_reports'), ('endorsement_requests')) AS pt(t)
     WHERE NOT EXISTS (
        SELECT 1
          FROM pg_index i
          JOIN pg_class     c ON c.oid = i.indrelid
          JOIN pg_namespace n ON n.oid = c.relnamespace
          JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = i.indkey[0]
         WHERE n.nspname = 'public'
           AND c.relname = pt.t
           AND i.indisprimary
           AND i.indnkeyatts = 1
           AND a.attname = 'id');

    IF missing <> '' THEN
        RAISE EXCEPTION 'PRECONDITION FAILED - the schema does not match the audited snapshot:%', missing;
    END IF;
    IF bad_pk IS NOT NULL THEN
        RAISE EXCEPTION 'PRECONDITION FAILED - these parent tables lack a single-column primary key named "id": %', bad_pk;
    END IF;

    RAISE NOTICE 'M01 Section 0 preconditions OK.';
END $m01_pre$;
-- ===========================================================================
-- SECTION 1A - FATAL ORPHAN GUARDS  (16 checks, run before any DDL)
-- ---------------------------------------------------------------------------
-- One entry per foreign key that Section 2 actually creates. Each entry reports
-- the exact child column, the parent table and up to 20 offending primary keys.
-- If ANY of these 16 checks finds an orphan, the migration aborts and NOTHING is
-- created. No row is ever deleted or modified to satisfy a constraint.
--
-- NULL HANDLING: a NULL referencing value is NOT an orphan. Every entry filters
-- "c.<col> IS NOT NULL", which is exactly MATCH SIMPLE semantics - the default
-- for a single-column foreign key, under which a NULL referencing value always
-- satisfies the constraint. So the guard models the constraint it protects.
-- Verified nullability in rpis_db:
--   * provably redundant (column is NOT NULL): activity_logs.user_id,
--     project_reports.proposal_id, quarterly_progress_reports.proposal_id
--   * essential (column is nullable): the other 13 entries
-- The filter is applied uniformly so NULL can never be read as an orphan.
-- ===========================================================================
DO $m01_guards$
DECLARE
    entry    record;
    n        bigint;
    sample   text;
    failures text := '';
BEGIN
    FOR entry IN
        SELECT * FROM (VALUES
            ('proposals',                     'proposal_announcement_id',      'proposal_announcements'),
            ('quarterly_progress_reports',    'proposal_id',                   'proposals'),
            ('quarterly_progress_reports',    'created_by',                    'users'),
            ('quarterly_progress_reports',    'submitted_by',                  'users'),
            ('quarterly_progress_reports',    'copied_from_report_id',         'quarterly_progress_reports'),
            ('project_reports',               'proposal_id',                   'proposals'),
            ('project_reports',               'submitted_by',                  'users'),
            ('project_reports',               'reviewed_by',                   'users'),
            ('activity_logs',                 'user_id',                       'users'),
            ('proposal_section_remarks',      'reviewer_id',                   'users'),
            ('ovcaf_validations',             'released_by',                   'users'),
            ('dean_assignments',              'created_by',                    'users'),
            ('dean_assignments',              'revoked_by',                    'users'),
            ('proposal_announcement_updates', 'actor_id',                      'users'),
            ('proposals',                     'returned_by_user_id',           'users'),
            ('proposals',                     'revision_forwarded_by_user_id', 'users')
        ) AS t(child_table, child_col, parent_table)
    LOOP
        EXECUTE format(
            'SELECT count(*) FROM public.%I c WHERE c.%I IS NOT NULL '
            'AND NOT EXISTS (SELECT 1 FROM public.%I p WHERE p.id = c.%I)',
            entry.child_table, entry.child_col, entry.parent_table, entry.child_col)
        INTO n;

        IF n > 0 THEN
            EXECUTE format(
                'SELECT string_agg(s.id::text, '', '' ORDER BY s.id) FROM ('
                '  SELECT c.id FROM public.%I c WHERE c.%I IS NOT NULL '
                '  AND NOT EXISTS (SELECT 1 FROM public.%I p WHERE p.id = c.%I) '
                '  ORDER BY c.id LIMIT 20) s',
                entry.child_table, entry.child_col, entry.parent_table, entry.child_col)
            INTO sample;

            failures := failures || format(
                E'\n  - %s.%s -> %s(id): %s orphan row(s); offending id(s) (max 20): %s',
                entry.child_table, entry.child_col, entry.parent_table, n,
                coalesce(sample, '(none)'));
        ELSE
            RAISE NOTICE 'FATAL guard OK: %.% -> %(id)',
                entry.child_table, entry.child_col, entry.parent_table;
        END IF;
    END LOOP;

    IF failures <> '' THEN
        RAISE EXCEPTION
            'FATAL ORPHAN GUARDS FAILED (16 fatal checks). No constraint was created and '
            'no row was modified. Correct the data (or approve a documented correction), '
            'then re-run:%', failures;
    END IF;

    RAISE NOTICE 'M01 Section 1A: all 16 fatal orphan guards passed.';
END $m01_guards$;
-- ===========================================================================
-- SECTION 1B - NON-FATAL DIAGNOSTIC (1 check, NEVER aborts the migration)
-- ---------------------------------------------------------------------------
-- endorsement_request_events.actor_id is deliberately EXCLUDED from this
-- migration's foreign keys, so it must not be in the fatal aggregation above: a
-- fatal check here would make the whole migration impossible to run.
--
-- CONFIRMED STATE (read-only, 2026-11-08): 2 orphan rows, ids 11 and 12, both
-- with actor_id = 79 ("Probe Dean Creator"), which no longer exists - users.id
-- currently spans 1..76. The account was deleted while its audit events remained.
--
-- THIS BLOCK ONLY REPORTS. It does not update, delete or null those rows, and it
-- does not stop the migration. The two rows are preserved exactly as they are.
-- The foreign key is intentionally not created, so the gap is documented and
-- visible rather than hidden.
-- ===========================================================================
DO $m01_diag$
DECLARE
    n      bigint;
    sample text;
BEGIN
    EXECUTE 'SELECT count(*) FROM public.endorsement_request_events c '
            'WHERE c.actor_id IS NOT NULL '
            'AND NOT EXISTS (SELECT 1 FROM public.users p WHERE p.id = c.actor_id)'
    INTO n;

    IF n > 0 THEN
        EXECUTE 'SELECT string_agg(s.id::text, '', '' ORDER BY s.id) FROM ('
                '  SELECT c.id FROM public.endorsement_request_events c '
                '   WHERE c.actor_id IS NOT NULL '
                '   AND NOT EXISTS (SELECT 1 FROM public.users p WHERE p.id = c.actor_id) '
                '   ORDER BY c.id LIMIT 20) s'
        INTO sample;

        RAISE WARNING
            'NON-FATAL DIAGNOSTIC - endorsement_request_events.actor_id -> users(id): % orphan row(s); offending id(s): %. These rows are PRESERVED and the foreign key is NOT created (approved decision). This warning does not stop the migration.',
            n, coalesce(sample, '(none)');
    ELSE
        RAISE NOTICE 'DIAGNOSTIC OK: endorsement_request_events.actor_id has no orphans (the foreign key is still not created by design).';
    END IF;
END $m01_diag$;
-- ===========================================================================
-- SECTION 2 - FOREIGN KEYS  (16 constraints)
-- ---------------------------------------------------------------------------
-- Names were checked against the 33 existing constraint names: no collision.
-- The child columns stay plain BIGINT in the JPA entities on purpose. They must
-- NOT also be mapped as @ManyToOne, otherwise the dev profile (ddl-auto=update)
-- would add a second, duplicate foreign key with an auto-generated name.
--
-- The guard below detects an EXISTING relationship by (child table, child
-- column -> parent table, id), not by constraint name. Feature migrations
-- (notably migration-add-quarterly-progress-reports.sql) create anonymous
-- <table>_<column>_fkey constraints; a name-only guard did not see those and
-- added a second, named constraint for the same relationship. The delete
-- action of the surviving constraint is normalised by
-- migration-norm-03-fk-consolidation.sql.
-- ===========================================================================
DO $m01_fks$
DECLARE
    entry record;
BEGIN
    FOR entry IN
        SELECT * FROM (VALUES
            ('fk_proposals_proposal_announcement',       'proposals',                     'proposal_announcement_id',      'proposal_announcements',     'RESTRICT'),
            ('fk_qpr_proposal',                          'quarterly_progress_reports',    'proposal_id',                   'proposals',                  'RESTRICT'),
            ('fk_qpr_created_by',                        'quarterly_progress_reports',    'created_by',                    'users',                      'SET NULL'),
            ('fk_qpr_submitted_by',                      'quarterly_progress_reports',    'submitted_by',                  'users',                      'SET NULL'),
            ('fk_qpr_copied_from',                       'quarterly_progress_reports',    'copied_from_report_id',         'quarterly_progress_reports', 'SET NULL'),
            ('fk_project_reports_proposal',              'project_reports',               'proposal_id',                   'proposals',                  'RESTRICT'),
            ('fk_project_reports_submitted_by',          'project_reports',               'submitted_by',                  'users',                      'SET NULL'),
            ('fk_project_reports_reviewed_by',           'project_reports',               'reviewed_by',                   'users',                      'SET NULL'),
            ('fk_activity_logs_user',                    'activity_logs',                 'user_id',                       'users',                      'RESTRICT'),
            ('fk_psr_reviewer',                          'proposal_section_remarks',      'reviewer_id',                   'users',                      'SET NULL'),
            ('fk_ovcaf_released_by',                     'ovcaf_validations',             'released_by',                   'users',                      'SET NULL'),
            ('fk_dean_assignments_created_by',           'dean_assignments',              'created_by',                    'users',                      'SET NULL'),
            ('fk_dean_assignments_revoked_by',           'dean_assignments',              'revoked_by',                    'users',                      'SET NULL'),
            ('fk_pau_actor',                             'proposal_announcement_updates', 'actor_id',                      'users',                      'SET NULL'),
            ('fk_proposals_returned_by_user',            'proposals',                     'returned_by_user_id',           'users',                      'SET NULL'),
            ('fk_proposals_revision_forwarded_by_user',  'proposals',                     'revision_forwarded_by_user_id', 'users',                      'SET NULL')
        ) AS t(conname, child_table, child_col, parent_table, on_delete)
    LOOP
        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint c
              JOIN pg_class      t  ON t.oid = c.conrelid
              JOIN pg_namespace  ns ON ns.oid = t.relnamespace
              JOIN pg_class      pt ON pt.oid = c.confrelid
             WHERE c.contype = 'f'
               AND ns.nspname = 'public'
               AND t.relname  = entry.child_table
               AND pt.relname = entry.parent_table
               AND array_length(c.conkey, 1) = 1
               AND c.conkey[1]  = (SELECT a.attnum FROM pg_attribute a
                                    WHERE a.attrelid = c.conrelid
                                      AND a.attname  = entry.child_col)
               AND c.confkey[1] = (SELECT a.attnum FROM pg_attribute a
                                    WHERE a.attrelid = c.confrelid
                                      AND a.attname  = 'id'))
        THEN
            EXECUTE format(
                'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) '
                'REFERENCES public.%I(id) ON DELETE %s',
                entry.child_table, entry.conname, entry.child_col,
                entry.parent_table, entry.on_delete);
        END IF;
    END LOOP;
END $m01_fks$;
-- ===========================================================================
-- SECTION 3 - INDEXES  (2 indexes)
-- ---------------------------------------------------------------------------
-- Only the two that the repository's own migrations document but that are ABSENT
-- from live rpis_db. Each is justified by a real query:
--   idx_users_role         - UserRepository.findByRoleIn (every status-change
--                            notification). Verified absent: users has only
--                            users_pkey and users_email_key.
--   idx_psr_proposal_field - proposal_section_remarks(proposal_id, field_key),
--                            documented in migration-add-field-level-remarks.sql.
-- NOTE: adding a foreign key creates NO index in PostgreSQL (unlike MySQL), so
-- Section 2 adds zero indexes.
-- Deliberately NOT created (would be redundant):
--   proposal_versions(proposal_id)  - already a prefix of uq_proposal_versions
--   activity_logs(user_id)          - already a prefix of idx_activity_logs_user_created
--   notifications(proposal_id)      - no repository query reads that column
-- ===========================================================================
CREATE INDEX IF NOT EXISTS idx_users_role
    ON public.users (role);

CREATE INDEX IF NOT EXISTS idx_psr_proposal_field
    ON public.proposal_section_remarks (proposal_id, field_key);

-- ===========================================================================
-- SECTION 4 - CHECK CONSTRAINTS  (15 constraints, all NOT VALID)
-- ---------------------------------------------------------------------------
-- Every allowed-value list was taken from the values actually stored in rpis_db
-- AND from every literal the backend writes. Verified: 0 existing rows violate
-- any of them. They are added NOT VALID, so the statement cannot fail on data and
-- every existing record is preserved untouched; new and updated rows are
-- enforced immediately.
--
-- chk_certifications_status is deliberately NOT created. rpis_db already has a
-- CHECK with that exact name on certifications enforcing the identical values
--   CHECK (status::text = ANY (ARRAY['ISSUED','SUPERSEDED']::text[]))
-- so creating it would be a no-op and would overstate this file's count. That is
-- why this section creates 15 constraints, not 16, and why the verification block
-- below lists the 15 names explicitly instead of matching a pattern.
--
-- Deliberately NOT constrained, with the reason:
--   proposals.status            - no complete list is knowable: ProposalService
--                                 line 415 stores the client-supplied status via
--                                 request.getStatus().toUpperCase(), and
--                                 PUT /api/proposals/{id}/status accepts any
--                                 string. A whitelist would fake a state machine.
--   proposals.reviewed_by_position - three incompatible vocabularies coexist
--                                 (RPS_STAFF/RPS_DIRECTOR in the data,
--                                 "RPS Staff"/"RPS Director"/"REC"/"Office of the
--                                 Chancellor" written by SyntheticTestDataSeeder).
--   endorsement_request_events.event_type - the complete literal set could not be
--                                 enumerated from the audit call sites.
--   project_reports.period      - client-supplied free string.
-- ===========================================================================
DO $m01_checks$
DECLARE
    entry record;
BEGIN
    FOR entry IN
        SELECT * FROM (VALUES
            ('users', 'chk_users_role',
             $$role IN ('PROPONENT','RPS_ADMIN','RPS_STAFF','DEAN','OVCRIGE','OVCAF','REC','OC')$$),
            ('users', 'chk_users_status',
             $$status IS NULL OR status IN ('PENDING','APPROVED','REJECTED','INACTIVE')$$),
            ('notifications', 'chk_notifications_type',
             $$type IS NULL OR type IN ('PROPOSAL_UPDATE','REVIEW_REQUEST','APPROVAL','REVISION','REJECTION','FUNDING','ENDORSEMENT','PROPOSAL_ANNOUNCEMENT')$$),
            ('activity_logs', 'chk_activity_logs_status',
             $$status IN ('SUCCESS','FAILED')$$),
            ('activity_logs', 'chk_activity_logs_action',
             $$action IN ('ACCOUNT_CREATED','ACCOUNT_UPDATED','ACCOUNT_APPROVED','ACCOUNT_REJECTED','ROLE_CHANGED','PASSWORD_CHANGED','PROFILE_UPDATED','CALL_CREATED','CALL_UPDATED','CALL_OPENED','CALL_CLOSED','SETTINGS_UPDATED')$$),
            ('activity_logs', 'chk_activity_logs_module',
             $$module IN ('User Accounts','Profile','Calls for Proposal','System Settings')$$),
            ('certifications', 'chk_certifications_document_type',
             $$document_type IN ('ENDORSEMENT_LETTER')$$),
            ('proposal_section_remarks', 'chk_psr_status',
             $$status IN ('ACTIVE','SUPERSEDED')$$),
            ('quarterly_progress_reports', 'chk_qpr_status',
             $$status IN ('DRAFT','SUBMITTED')$$),
            ('quarterly_progress_reports', 'chk_qpr_report_type',
             $$report_type IS NULL OR report_type IN ('QUARTERLY_PROGRESS','TERMINAL_REPORT')$$),
            ('dean_assignments', 'chk_dean_assignments_status',
             $$status IN ('ACTIVE','REVOKED')$$),
            ('ovcaf_validations', 'chk_ovcaf_decision',
             $$decision IN ('APPROVED_ENDORSED','RETURNED_FOR_REVISION','REJECTED')$$),
            ('proposal_reviews', 'chk_proposal_reviews_decision',
             $$decision IS NULL OR decision IN ('APPROVED','REJECTED','REVISION_REQUIRED','REVISION')$$),
            ('project_reports', 'chk_project_reports_report_type',
             $$report_type IN ('QUARTERLY_PROGRESS','FINANCIAL')$$),
            ('project_reports', 'chk_project_reports_review_status',
             $$review_status IS NULL OR review_status IN ('PENDING','APPROVED','RETURNED')$$)
        ) AS t(tbl, conname, expr)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
              JOIN pg_class     t  ON t.oid = c.conrelid
              JOIN pg_namespace ns ON ns.oid = t.relnamespace
             WHERE c.conname = entry.conname AND ns.nspname = 'public')
        THEN
            -- NOTE: the inner double-dollar delimiters below are legal because the
            -- enclosing block uses a unique dollar-quote tag, not double-dollar.
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I CHECK (%s) NOT VALID',
                           entry.tbl, entry.conname, entry.expr);
        END IF;
    END LOOP;
END $m01_checks$;

COMMIT;

-- ===========================================================================
-- VERIFICATION - counts ONLY this file's own named objects, so the output is
-- identical whether or not 02 and 03 have already run. No global total is
-- asserted, and no pattern match is used: the 15 CHECK names are listed
-- explicitly so the pre-existing chk_certifications_status can never be counted.
-- ===========================================================================
SELECT 'M01 fk constraints present (expected 16)' AS check_name, count(*)::text AS value
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace AND contype = 'f'
   AND conname IN (
       'fk_proposals_proposal_announcement','fk_qpr_proposal','fk_qpr_created_by',
       'fk_qpr_submitted_by','fk_qpr_copied_from','fk_project_reports_proposal',
       'fk_project_reports_submitted_by','fk_project_reports_reviewed_by',
       'fk_activity_logs_user','fk_psr_reviewer','fk_ovcaf_released_by',
       'fk_dean_assignments_created_by','fk_dean_assignments_revoked_by','fk_pau_actor',
       'fk_proposals_returned_by_user','fk_proposals_revision_forwarded_by_user')
UNION ALL
SELECT 'M01 check constraints present (expected 15)', count(*)::text
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace AND contype = 'c'
   AND conname IN (
       'chk_users_role','chk_users_status','chk_notifications_type',
       'chk_activity_logs_status','chk_activity_logs_action','chk_activity_logs_module',
       'chk_certifications_document_type','chk_psr_status','chk_qpr_status',
       'chk_qpr_report_type','chk_dean_assignments_status','chk_ovcaf_decision',
       'chk_proposal_reviews_decision','chk_project_reports_report_type',
       'chk_project_reports_review_status')
UNION ALL
SELECT 'M01 check constraints still NOT VALID (expected 15; VALIDATE is a separate step)',
       count(*)::text
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace AND contype = 'c' AND NOT convalidated
   AND conname IN (
       'chk_users_role','chk_users_status','chk_notifications_type',
       'chk_activity_logs_status','chk_activity_logs_action','chk_activity_logs_module',
       'chk_certifications_document_type','chk_psr_status','chk_qpr_status',
       'chk_qpr_report_type','chk_dean_assignments_status','chk_ovcaf_decision',
       'chk_proposal_reviews_decision','chk_project_reports_report_type',
       'chk_project_reports_review_status')
UNION ALL
SELECT 'M01 indexes present (expected 2)', count(*)::text
  FROM pg_indexes
 WHERE schemaname = 'public' AND indexname IN ('idx_users_role','idx_psr_proposal_field')
UNION ALL
SELECT 'no foreign key created for endorsement_request_events.actor_id (expected 0)',
       count(*)::text
  FROM pg_constraint
 WHERE connamespace = 'public'::regnamespace AND contype = 'f'
   AND conrelid = to_regclass('public.endorsement_request_events')
   AND pg_get_constraintdef(oid) ILIKE '%actor_id%'
UNION ALL
SELECT 'endorsement_request_events rows with actor_id = 79 (must still be 2 - never nulled)',
       (SELECT count(*)::text FROM public.endorsement_request_events WHERE actor_id = 79)
UNION ALL
SELECT 'schema totals (informational, NOT asserted)',
       (SELECT count(*)::text FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype='f')
       || ' fk / '
       || (SELECT count(*)::text FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype='c')
       || ' check / '
       || (SELECT count(*)::text FROM pg_indexes WHERE schemaname='public')
       || ' index / '
       || (SELECT count(*)::text FROM information_schema.tables
            WHERE table_schema='public' AND table_type='BASE TABLE') || ' tables';
