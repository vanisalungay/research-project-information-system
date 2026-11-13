-- =============================================================================
-- RPIS Migration NORM-04: proposal status foreign keys - rpis_db
-- =============================================================================
-- Adds exactly three foreign keys:
--
--   proposals.status                     -> proposal_statuses.code
--        fk_proposals_status
--   proposal_status_history.from_status  -> proposal_statuses.code
--        fk_psh_from_status
--   proposal_status_history.to_status    -> proposal_statuses.code
--        fk_psh_to_status
--
-- All three use ON DELETE RESTRICT / ON UPDATE NO ACTION / MATCH SIMPLE and are
-- NOT DEFERRABLE. A status code is reference data: a code still used by a
-- proposal or by a history row must not be deleted, and codes are not renamed by
-- the application.
--
-- PREREQUISITE LOOKUP ROW
--   The current backend persists a Proponent submission as the literal code
--   'FOR REVIEW' (ProposalStatus.toStoredStatus maps the client's 'SUBMITTED'
--   to 'FOR REVIEW'), and records that value in proposal_status_history. The
--   status table seeded by migration-03 contains the legacy 'SUBMITTED' spelling
--   but not 'FOR REVIEW'. Without a lookup row for the current spelling, the new
--   FKs would reject every new submission.
--
--   Section 3 therefore adds that ONE reference row. It is INSERT-only and uses
--   ON CONFLICT (code) DO NOTHING, so it never updates an existing lookup row and
--   never touches a proposal, a proposal status or a history row. 'SUBMITTED'
--   is left exactly as it is; existing proposals keep their stored values.
--
-- EXISTING FOREIGN KEYS
--   An existing FK is compared by its ACTUAL DEFINITION, not by its name:
--   referencing schema/table/column, referenced schema/table/column, ON DELETE,
--   ON UPDATE, MATCH type, deferrability and validation state.
--
--   * Exactly one equivalent FK whose definition already matches is kept. If it
--     has a different name it is renamed to the agreed NORM-04 name, so a valid
--     constraint is never dropped and recreated merely because of its name.
--   * Exactly one equivalent FK whose definition is incompatible (for example
--     ON DELETE CASCADE, ON UPDATE CASCADE, MATCH FULL, DEFERRABLE or NOT VALID)
--     is replaced with the intended definition.
--   * More than one equivalent FK is an explicit abort: the migration will not
--     silently drop duplicates it cannot safely restore.
--   * A foreign key on the same child column that references a different parent
--     is an explicit abort (conflicting relationship).
--   * A different constraint already occupying the agreed name is an explicit
--     abort.
--
-- ROLLBACK REGISTRY
--   Section 2 creates public.database_migration_rollback_log, a small bookkeeping table.
--   Every constraint NORM-04 creates, renames or replaces - and every index it
--   creates - is recorded there, including the original name, definition and
--   comment for a replaced constraint. The rollback reads this table to restore
--   the pre-NORM-04 state exactly. The rollback drops the registry at the end.
--
-- INDEXES
--   Existing indexes are accepted only when their actual definition is a plain,
--   valid, non-unique btree on the expected public table and column. A same-named
--   index with a different definition is an explicit abort, never silently
--   accepted. An index NORM-04 creates is recorded in the registry so the
--   rollback can remove it; a pre-existing index is never dropped by rollback.
--
-- WHAT THIS MIGRATION DOES NOT DO
--   * It does not delete, overwrite or change any proposal, history or lookup row.
--   * It does not rename or change any canonical status code.
--   * It does not touch user status, certification status, endorsement request
--     status or quarterly report status.
--   * It does not modify an existing migration.
--
-- SAFETY
--   * Additive only: one lookup row, one registry table, three constraints and
--     the missing child indexes. No proposal/history row is rewritten.
--   * Preconditions abort before any constraint/index DDL if a non-NULL status
--     value has no matching proposal_statuses.code. Existing data is verified,
--     never repaired.
--   * Idempotent: the lookup insert conflicts on the primary key, constraints are
--     detected by full relationship/definition, and indexes by name AND structure.
--     Re-running the file is a no-op.
--
-- ORDER
--   Run after migration-03-db-audit-status-history.sql (creates the lookup and
--   history tables) and after migration-norm-03-fk-consolidation.sql. It does not
--   depend on any other NORM migration.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

-- ===========================================================================
-- SECTION 1 - STRUCTURE PRECONDITIONS
-- Abort with a readable message instead of a raw "relation does not exist".
-- ===========================================================================
DO $norm04_tables$
BEGIN
    IF to_regclass('public.proposal_statuses') IS NULL THEN
        RAISE EXCEPTION 'NORM-04 PRECONDITION FAILED: public.proposal_statuses does not exist. Apply migration-03-db-audit-status-history.sql first.';
    END IF;
    IF to_regclass('public.proposals') IS NULL THEN
        RAISE EXCEPTION 'NORM-04 PRECONDITION FAILED: public.proposals does not exist.';
    END IF;
    IF to_regclass('public.proposal_status_history') IS NULL THEN
        RAISE EXCEPTION 'NORM-04 PRECONDITION FAILED: public.proposal_status_history does not exist. Apply migration-03-db-audit-status-history.sql first.';
    END IF;
END $norm04_tables$;

-- ===========================================================================
-- SECTION 2 - ROLLBACK REGISTRY
-- Records exactly what NORM-04 created, renamed or replaced so the rollback can
-- restore the prior state. This table holds migration metadata only; it is
-- dropped by rollback-norm-04-proposal-status-fks.sql.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS public.database_migration_rollback_log (
    id                  BIGSERIAL PRIMARY KEY,
    object_kind         VARCHAR(10) NOT NULL
                            CHECK (object_kind IN ('constraint', 'index')),
    child_table         VARCHAR(63) NOT NULL,
    child_column        VARCHAR(63) NOT NULL,
    agreed_name         VARCHAR(63) NOT NULL,
    action              VARCHAR(10) NOT NULL
                            CHECK (action IN ('created', 'renamed', 'replaced')),
    original_name       VARCHAR(63),
    original_definition TEXT,
    original_comment    TEXT,
    recorded_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT norm04_registry_unique
        UNIQUE (object_kind, child_table, agreed_name)
);

-- ===========================================================================
-- SECTION 3 - THE CURRENT APPLICATION SPELLING MUST BE A VALID CODE
-- Same stage as SUBMITTED: "For RPS Review" (the label every screen shows for
-- both spellings). order 20 keeps it beside the legacy code; bucket submitted.
-- ON CONFLICT DO NOTHING means an existing row - including one an operator has
-- already added with different descriptive text - is left untouched.
-- ===========================================================================
INSERT INTO public.proposal_statuses (code, stage_label, stage_order, bucket, active, notes)
VALUES
    ('FOR REVIEW', 'For RPS Review', 20, 'submitted', TRUE,
     'Current application spelling of the legacy SUBMITTED state. Added by NORM-04 so the proposal status foreign keys accept new submissions.')
ON CONFLICT (code) DO NOTHING;

-- ===========================================================================
-- SECTION 4 - DATA PRECONDITION
-- Every NON-NULL value in the three columns must already resolve to a lookup
-- code before a constraint is added. The migration never repairs data: if this
-- aborts, an owner must decide what the unmatched value should be, and no
-- constraint, index or row has been changed (the transaction rolls back).
-- NULL is allowed by all three relationships and is not counted here.
-- ===========================================================================
DO $norm04_guard$
DECLARE
    r   record;
    n   bigint;
    bad text := '';
BEGIN
    FOR r IN SELECT * FROM (VALUES
        ('proposals',                'status'),
        ('proposal_status_history',  'from_status'),
        ('proposal_status_history',  'to_status')
    ) AS t(child_table, child_col)
    LOOP
        EXECUTE format(
            'SELECT count(*) FROM public.%I c '
            'WHERE c.%I IS NOT NULL '
            '  AND NOT EXISTS (SELECT 1 FROM public.proposal_statuses s WHERE s.code = c.%I)',
            r.child_table, r.child_col, r.child_col) INTO n;
        IF n > 0 THEN
            bad := bad || format(E'\n  - %s.%s : %s row(s) have no matching proposal_statuses.code',
                                 r.child_table, r.child_col, n);
        END IF;
    END LOOP;

    IF bad <> '' THEN
        RAISE EXCEPTION 'NORM-04 PRECONDITION FAILED - unmatched proposal status values:%', bad;
    END IF;

    RAISE NOTICE 'NORM-04 preconditions OK: every non-NULL status value resolves to proposal_statuses.code.';
END $norm04_guard$;

-- ===========================================================================
-- SECTION 5 - THE THREE FOREIGN KEYS
-- Detection is by relationship AND full definition:
--   child schema/table/column, parent schema/table/column,
--   ON DELETE = RESTRICT, ON UPDATE = NO ACTION, MATCH = SIMPLE,
--   NOT DEFERRABLE, validated.
--
-- Actions:
--   0 equivalent FKs  -> create the agreed constraint (recorded as 'created').
--   1 equivalent FK, definition matches, agreed name -> keep (no record).
--   1 equivalent FK, definition matches, other name  -> rename (recorded as
--                                                       'renamed').
--   1 equivalent FK, definition differs              -> replace (recorded as
--                                                       'replaced', with the
--                                                       original name/definition
--                                                       so rollback can restore).
--   >1 equivalent FKs                                -> explicit abort.
-- ===========================================================================
DO $norm04_fks$
DECLARE
    e                 record;
    child_oid         oid;
    parent_oid        oid := 'public.proposal_statuses'::regclass;
    child_attnum      smallint;
    parent_attnum     smallint;
    eq_count          integer;
    eq_names          text;
    conflicting_names text;
    agreed_occupied   boolean;
    original          record;
BEGIN
    SELECT a.attnum INTO parent_attnum
      FROM pg_attribute a
     WHERE a.attrelid = parent_oid
       AND a.attname  = 'code'
       AND NOT a.attisdropped;

    IF parent_attnum IS NULL THEN
        RAISE EXCEPTION 'NORM-04 PRECONDITION FAILED: public.proposal_statuses.code does not exist.';
    END IF;

    FOR e IN
        SELECT * FROM (VALUES
            ('proposals',               'status',      'fk_proposals_status'),
            ('proposal_status_history', 'from_status', 'fk_psh_from_status'),
            ('proposal_status_history', 'to_status',   'fk_psh_to_status')
        ) AS t(child_table, child_col, conname)
    LOOP
        child_oid := format('public.%I', e.child_table)::regclass;

        SELECT a.attnum INTO child_attnum
          FROM pg_attribute a
         WHERE a.attrelid = child_oid
           AND a.attname  = e.child_col
           AND NOT a.attisdropped;

        IF child_attnum IS NULL THEN
            RAISE EXCEPTION 'NORM-04 PRECONDITION FAILED: column public.%.% does not exist.',
                e.child_table, e.child_col;
        END IF;

        -- All foreign keys that already describe the intended relationship,
        -- whatever their names. Parent schema/table/column are checked explicitly.
        SELECT count(*)::int,
               string_agg(c.conname, ', ' ORDER BY c.conname)
          INTO eq_count, eq_names
          FROM pg_constraint c
          JOIN pg_class     t  ON t.oid  = c.conrelid
          JOIN pg_namespace tn ON tn.oid = t.relnamespace
          JOIN pg_class     pt ON pt.oid = c.confrelid
          JOIN pg_namespace pn ON pn.oid = pt.relnamespace
         WHERE c.contype = 'f'
           AND tn.nspname = 'public'
           AND t.relname  = e.child_table
           AND pn.nspname = 'public'
           AND pt.relname = 'proposal_statuses'
           AND c.confrelid = parent_oid
           AND array_length(c.conkey, 1) = 1
           AND array_length(c.confkey, 1) = 1
           AND c.conkey[1]  = child_attnum
           AND c.confkey[1] = parent_attnum;

        IF eq_count > 1 THEN
            RAISE EXCEPTION 'NORM-04 ABORTED: % foreign keys already constrain public.%.% to public.proposal_statuses(code): %. Resolve the duplicates manually; NORM-04 will not drop pre-existing constraints it cannot safely restore.',
                eq_count, e.child_table, e.child_col, eq_names;
        END IF;

        -- A foreign key on the same child column that references a different
        -- parent is a conflicting relationship, not something to duplicate.
        SELECT string_agg(format('%s -> %s', c.conname, c.confrelid::regclass),
                          ', ' ORDER BY c.conname)
          INTO conflicting_names
          FROM pg_constraint c
         WHERE c.contype = 'f'
           AND c.conrelid = child_oid
           AND child_attnum = ANY (c.conkey)
           AND NOT (c.confrelid = parent_oid
                    AND array_length(c.confkey, 1) = 1
                    AND c.confkey[1] = parent_attnum);

        IF conflicting_names IS NOT NULL THEN
            RAISE EXCEPTION 'NORM-04 ABORTED: public.%.% already participates in another foreign key: %. Resolve the conflicting relationship manually.',
                e.child_table, e.child_col, conflicting_names;
        END IF;

        -- The agreed name must be free, unless it already names the one
        -- equivalent FK inspected below.
        SELECT EXISTS (
            SELECT 1
              FROM pg_constraint c
             WHERE c.conrelid = child_oid
               AND c.conname  = e.conname
               AND NOT (c.contype = 'f'
                        AND c.confrelid = parent_oid
                        AND array_length(c.conkey, 1) = 1
                        AND array_length(c.confkey, 1) = 1
                        AND c.conkey[1]  = child_attnum
                        AND c.confkey[1] = parent_attnum)
        ) INTO agreed_occupied;

        IF agreed_occupied THEN
            RAISE EXCEPTION 'NORM-04 ABORTED: constraint name % already exists on public.% and does not describe %.% -> public.proposal_statuses(code).',
                e.conname, e.child_table, e.child_table, e.child_col;
        END IF;

        IF eq_count = 0 THEN
            EXECUTE format(
                'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) '
                'REFERENCES public.proposal_statuses(code) '
                'ON DELETE RESTRICT ON UPDATE NO ACTION',
                e.child_table, e.conname, e.child_col);
            INSERT INTO public.database_migration_rollback_log
                (object_kind, child_table, child_column, agreed_name, action)
            VALUES ('constraint', e.child_table, e.child_col, e.conname, 'created')
            ON CONFLICT (object_kind, child_table, agreed_name) DO NOTHING;
            RAISE NOTICE 'NORM-04: created % (%.% -> proposal_statuses.code)',
                e.conname, e.child_table, e.child_col;
        ELSE
            SELECT c.conname,
                   pg_get_constraintdef(c.oid) AS definition,
                   obj_description(c.oid, 'pg_constraint') AS comment,
                   (c.confdeltype  = 'r'
                    AND c.confupdtype = 'a'
                    AND c.confmatchtype = 's'
                    AND NOT c.condeferrable
                    AND NOT c.condeferred
                    AND c.convalidated) AS definition_matches
              INTO original
              FROM pg_constraint c
              JOIN pg_class     t  ON t.oid  = c.conrelid
              JOIN pg_namespace tn ON tn.oid = t.relnamespace
              JOIN pg_class     pt ON pt.oid = c.confrelid
              JOIN pg_namespace pn ON pn.oid = pt.relnamespace
             WHERE c.contype = 'f'
               AND tn.nspname = 'public'
               AND t.relname  = e.child_table
               AND pn.nspname = 'public'
               AND pt.relname = 'proposal_statuses'
               AND c.confrelid = parent_oid
               AND array_length(c.conkey, 1) = 1
               AND array_length(c.confkey, 1) = 1
               AND c.conkey[1]  = child_attnum
               AND c.confkey[1] = parent_attnum;

            IF original.conname = e.conname AND original.definition_matches THEN
                RAISE NOTICE 'NORM-04: % already exists and matches', e.conname;
            ELSIF original.definition_matches THEN
                EXECUTE format('ALTER TABLE public.%I RENAME CONSTRAINT %I TO %I',
                               e.child_table, original.conname, e.conname);
                INSERT INTO public.database_migration_rollback_log
                    (object_kind, child_table, child_column, agreed_name,
                     action, original_name)
                VALUES ('constraint', e.child_table, e.child_col, e.conname,
                        'renamed', original.conname)
                ON CONFLICT (object_kind, child_table, agreed_name) DO NOTHING;
                RAISE NOTICE 'NORM-04: renamed % to % (definition already correct)',
                    original.conname, e.conname;
            ELSE
                EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I',
                               e.child_table, original.conname);
                EXECUTE format(
                    'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) '
                    'REFERENCES public.proposal_statuses(code) '
                    'ON DELETE RESTRICT ON UPDATE NO ACTION',
                    e.child_table, e.conname, e.child_col);
                INSERT INTO public.database_migration_rollback_log
                    (object_kind, child_table, child_column, agreed_name,
                     action, original_name, original_definition, original_comment)
                VALUES ('constraint', e.child_table, e.child_col, e.conname,
                        'replaced', original.conname, original.definition,
                        original.comment)
                ON CONFLICT (object_kind, child_table, agreed_name) DO NOTHING;
                RAISE NOTICE 'NORM-04: replaced % (incompatible definition) with %',
                    original.conname, e.conname;
            END IF;
        END IF;
    END LOOP;
END $norm04_fks$;

-- ===========================================================================
-- SECTION 6 - CHILD-COLUMN INDEXES
-- PostgreSQL does not index a foreign key's child columns automatically. Each
-- existing index is accepted only if its ACTUAL definition is a plain btree on
-- the expected public table and column: same table, single key column, not
-- partial, not an expression, valid, non-unique. A same-named index with any
-- other definition is an explicit abort.
--
-- At the current table sizes (29 proposals / 35 history rows) a plain CREATE
-- INDEX is instant. On a large production table, build them separately with
-- CREATE INDEX CONCURRENTLY before applying this file; the verified-existence
-- branch then keeps them and records nothing.
-- ===========================================================================
DO $norm04_indexes$
DECLARE
    e          record;
    table_oid  oid;
    col_attnum smallint;
    idx_exists boolean;
    idx_ok     boolean;
BEGIN
    FOR e IN
        SELECT * FROM (VALUES
            ('proposals',               'status',      'idx_proposals_status'),
            ('proposal_status_history', 'from_status', 'idx_psh_from_status'),
            ('proposal_status_history', 'to_status',   'idx_psh_to_status')
        ) AS t(child_table, child_col, index_name)
    LOOP
        table_oid := format('public.%I', e.child_table)::regclass;

        SELECT a.attnum INTO col_attnum
          FROM pg_attribute a
         WHERE a.attrelid = table_oid
           AND a.attname  = e.child_col
           AND NOT a.attisdropped;

        SELECT EXISTS (
            SELECT 1
              FROM pg_class ic
              JOIN pg_namespace n ON n.oid = ic.relnamespace
             WHERE n.nspname = 'public'
               AND ic.relname = e.index_name
               AND ic.relkind = 'i'
        ) INTO idx_exists;

        IF idx_exists THEN
            SELECT (i.indrelid      = table_oid
                    AND i.indnkeyatts = 1
                    AND i.indnatts    = 1
                    AND i.indkey[0]   = col_attnum
                    AND i.indpred     IS NULL
                    AND i.indexprs    IS NULL
                    AND i.indisvalid
                    AND NOT i.indisunique
                    AND am.amname = 'btree')
              INTO idx_ok
              FROM pg_index i
              JOIN pg_class ic     ON ic.oid = i.indexrelid
              JOIN pg_namespace n  ON n.oid  = ic.relnamespace
              JOIN pg_am am        ON am.oid = ic.relam
             WHERE n.nspname = 'public'
               AND ic.relname = e.index_name;

            IF NOT COALESCE(idx_ok, false) THEN
                RAISE EXCEPTION 'NORM-04 ABORTED: index public.% already exists but is not a plain, valid, non-unique btree index on public.%.%. Refusing to accept an incorrectly defined index.',
                    e.index_name, e.child_table, e.child_col;
            END IF;
            RAISE NOTICE 'NORM-04: index % already exists and matches', e.index_name;
        ELSE
            EXECUTE format('CREATE INDEX %I ON public.%I (%I)',
                           e.index_name, e.child_table, e.child_col);
            INSERT INTO public.database_migration_rollback_log
                (object_kind, child_table, child_column, agreed_name, action)
            VALUES ('index', e.child_table, e.child_col, e.index_name, 'created')
            ON CONFLICT (object_kind, child_table, agreed_name) DO NOTHING;
            RAISE NOTICE 'NORM-04: created index % on %.%',
                e.index_name, e.child_table, e.child_col;
        END IF;
    END LOOP;
END $norm04_indexes$;

COMMIT;

-- ===========================================================================
-- VERIFICATION (read-only, after COMMIT)
-- ===========================================================================

-- 1) The three relationships, exactly as the database enforces them. The parent
--    schema is checked explicitly, not inferred from the table name.
SELECT tn.nspname            AS child_schema,
       t.relname             AS child_table,
       a.attname             AS child_column,
       pn.nspname            AS parent_schema,
       pt.relname            AS parent_table,
       pa.attname            AS parent_column,
       c.conname             AS constraint_name,
       CASE c.confdeltype
            WHEN 'r' THEN 'RESTRICT'
            WHEN 'a' THEN 'NO ACTION'
            WHEN 'c' THEN 'CASCADE'
            WHEN 'n' THEN 'SET NULL'
            WHEN 'd' THEN 'SET DEFAULT'
            ELSE c.confdeltype::text
       END                   AS on_delete,
       CASE c.confupdtype
            WHEN 'r' THEN 'RESTRICT'
            WHEN 'a' THEN 'NO ACTION'
            WHEN 'c' THEN 'CASCADE'
            WHEN 'n' THEN 'SET NULL'
            WHEN 'd' THEN 'SET DEFAULT'
            ELSE c.confupdtype::text
       END                   AS on_update,
       CASE c.confmatchtype
            WHEN 's' THEN 'SIMPLE'
            WHEN 'f' THEN 'FULL'
            WHEN 'p' THEN 'PARTIAL'
            ELSE c.confmatchtype::text
       END                   AS match_type,
       c.condeferrable       AS deferrable,
       c.condeferred         AS initially_deferred,
       c.convalidated        AS validated
  FROM pg_constraint c
  JOIN pg_class     t  ON t.oid  = c.conrelid
  JOIN pg_namespace tn ON tn.oid = t.relnamespace
  JOIN pg_class     pt ON pt.oid = c.confrelid
  JOIN pg_namespace pn ON pn.oid = pt.relnamespace
  JOIN pg_attribute a  ON a.attrelid  = c.conrelid  AND a.attnum  = c.conkey[1]
  JOIN pg_attribute pa ON pa.attrelid = c.confrelid AND pa.attnum = c.confkey[1]
 WHERE c.contype = 'f'
   AND tn.nspname = 'public'
   AND pn.nspname = 'public'
   AND pt.relname = 'proposal_statuses'
   AND (t.relname, a.attname) IN (
        ('proposals',               'status'),
        ('proposal_status_history', 'from_status'),
        ('proposal_status_history', 'to_status'))
 ORDER BY t.relname, a.attname;

-- 2) The indexes, verified by table/column/structure rather than by name alone.
SELECT t.relname       AS table_name,
       a.attname       AS column_name,
       ic.relname      AS index_name,
       am.amname       AS access_method,
       i.indisvalid    AS valid,
       i.indisunique   AS is_unique,
       (i.indpred IS NULL)   AS not_partial,
       (i.indexprs IS NULL)  AS simple_column
  FROM pg_index i
  JOIN pg_class     ic ON ic.oid = i.indexrelid
  JOIN pg_namespace n  ON n.oid  = ic.relnamespace
  JOIN pg_am        am ON am.oid = ic.relam
  JOIN pg_class     t  ON t.oid  = i.indrelid
  JOIN pg_attribute a  ON a.attrelid = i.indrelid AND a.attnum = i.indkey[0]
 WHERE n.nspname = 'public'
   AND ic.relname IN ('idx_proposals_status', 'idx_psh_from_status', 'idx_psh_to_status')
 ORDER BY ic.relname;

-- 3) Counts. Every "expected" value is the value a successful application has.
SELECT 'NORM-04 foreign keys present (expected 3)' AS check_name,
       count(*)::text AS value
  FROM pg_constraint c
  JOIN pg_class     t  ON t.oid = c.conrelid
  JOIN pg_namespace tn ON tn.oid = t.relnamespace
  JOIN pg_class     pt ON pt.oid = c.confrelid
  JOIN pg_namespace pn ON pn.oid = pt.relnamespace
 WHERE c.contype = 'f'
   AND tn.nspname = 'public'
   AND pn.nspname = 'public'
   AND pt.relname = 'proposal_statuses'
   AND c.conname IN ('fk_proposals_status', 'fk_psh_from_status', 'fk_psh_to_status')
UNION ALL
SELECT 'NORM-04 foreign keys validated (expected 3)',
       count(*)::text
  FROM pg_constraint c
  JOIN pg_class     t  ON t.oid = c.conrelid
  JOIN pg_namespace tn ON tn.oid = t.relnamespace
  JOIN pg_class     pt ON pt.oid = c.confrelid
  JOIN pg_namespace pn ON pn.oid = pt.relnamespace
 WHERE c.contype = 'f'
   AND tn.nspname = 'public'
   AND pn.nspname = 'public'
   AND pt.relname = 'proposal_statuses'
   AND c.convalidated
   AND c.conname IN ('fk_proposals_status', 'fk_psh_from_status', 'fk_psh_to_status')
UNION ALL
SELECT 'NORM-04 indexes present (expected 3)',
       count(*)::text
  FROM pg_class ic
  JOIN pg_namespace n ON n.oid = ic.relnamespace
 WHERE n.nspname = 'public'
   AND ic.relkind = 'i'
   AND ic.relname IN ('idx_proposals_status', 'idx_psh_from_status', 'idx_psh_to_status')
UNION ALL
SELECT 'FOR REVIEW lookup row present (expected 1)',
       count(*)::text
  FROM public.proposal_statuses
 WHERE code = 'FOR REVIEW'
UNION ALL
SELECT 'status values without a lookup code (expected 0)',
       ((SELECT count(*) FROM public.proposals p
          WHERE p.status IS NOT NULL
            AND NOT EXISTS (SELECT 1 FROM public.proposal_statuses s WHERE s.code = p.status))
        + (SELECT count(*) FROM public.proposal_status_history h
            WHERE h.from_status IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM public.proposal_statuses s WHERE s.code = h.from_status))
        + (SELECT count(*) FROM public.proposal_status_history h
            WHERE h.to_status IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM public.proposal_statuses s WHERE s.code = h.to_status)))::text
UNION ALL
SELECT 'registry rows (informational)',
       (SELECT count(*)::text FROM public.database_migration_rollback_log)
UNION ALL
SELECT 'schema totals (informational)',
       (SELECT count(*)::text FROM pg_constraint
         WHERE connamespace = 'public'::regnamespace AND contype = 'f') || ' fk / ' ||
       (SELECT count(*)::text FROM pg_indexes WHERE schemaname = 'public') || ' index / ' ||
       (SELECT count(*)::text FROM information_schema.tables
         WHERE table_schema = 'public' AND table_type = 'BASE TABLE') || ' tables';
