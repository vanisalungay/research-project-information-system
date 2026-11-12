-- =============================================================================
-- Rollback for NORM-04 (proposal status foreign keys) - rpis_db
-- =============================================================================
-- Reads public.database_migration_rollback_log (created by NORM-04) and restores the
-- exact pre-NORM-04 state:
--
--   * a constraint NORM-04 created           -> dropped
--   * a constraint NORM-04 renamed           -> renamed back to its original name
--   * a constraint NORM-04 replaced          -> the NORM-04 constraint is dropped
--                                               and the original constraint is
--                                               recreated with its recorded name,
--                                               definition (including NOT VALID /
--                                               deferrability) and comment
--   * an index NORM-04 created               -> dropped
--   * an index that already existed          -> never dropped
--
-- It deliberately does NOT drop a pre-existing idx_proposals_status index.
-- It deliberately does NOT delete the 'FOR REVIEW' lookup row added by NORM-04:
-- once the migration has been live, proposals or history rows may reference it;
-- deleting the row would require deciding what to do with those records. The row
-- is valid reference data for the legacy SUBMITTED state, so retaining it is
-- harmless. If an operator truly needs the pre-NORM-04 lookup table, they must
-- first confirm that no proposal or history row uses 'FOR REVIEW', then delete
-- that one row themselves.
--
-- No proposal, history or lookup row is updated or deleted by this script.
-- The registry table is removed at the end. If the registry is absent (NORM-04
-- was never applied, or this rollback already ran), the script is a no-op.
-- Safe to run repeatedly.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $norm04_rollback$
DECLARE
    r               record;
    child_oid       oid;
    parent_oid      oid := 'public.proposal_statuses'::regclass;
    parent_attnum   smallint;
    child_attnum    smallint;
    agreed_exists   boolean;
    original_exists boolean;
    agreed_matches  boolean;
    index_ok        boolean;
BEGIN
    IF to_regclass('public.database_migration_rollback_log') IS NULL THEN
        RAISE NOTICE 'NORM-04 rollback: no rollback registry found; nothing to undo.';
        RETURN;
    END IF;

    SELECT a.attnum INTO parent_attnum
      FROM pg_attribute a
     WHERE a.attrelid = parent_oid
       AND a.attname  = 'code'
       AND NOT a.attisdropped;

    -- ------------------------------------------------------------------
    -- Constraints
    -- ------------------------------------------------------------------
    FOR r IN
        SELECT *
          FROM public.database_migration_rollback_log
         WHERE object_kind = 'constraint'
         ORDER BY id
    LOOP
        child_oid := format('public.%I', r.child_table)::regclass;

        SELECT a.attnum INTO child_attnum
          FROM pg_attribute a
         WHERE a.attrelid = child_oid
           AND a.attname  = r.child_column
           AND NOT a.attisdropped;

        SELECT EXISTS (
            SELECT 1
              FROM pg_constraint c
             WHERE c.conrelid = child_oid
               AND c.conname  = r.agreed_name
        ) INTO agreed_exists;

        -- Never touch a constraint that no longer looks like the one NORM-04
        -- installed. An operator's later change must win over a blind rollback.
        IF agreed_exists THEN
            SELECT (c.contype = 'f'
                    AND c.confrelid = parent_oid
                    AND c.confdeltype  = 'r'
                    AND c.confupdtype  = 'a'
                    AND c.confmatchtype = 's'
                    AND NOT c.condeferrable
                    AND NOT c.condeferred
                    AND c.convalidated
                    AND array_length(c.conkey, 1) = 1
                    AND array_length(c.confkey, 1) = 1
                    AND c.conkey[1]  = child_attnum
                    AND c.confkey[1] = parent_attnum)
              INTO agreed_matches
              FROM pg_constraint c
             WHERE c.conrelid = child_oid
               AND c.conname  = r.agreed_name;

            IF NOT COALESCE(agreed_matches, false) THEN
                RAISE EXCEPTION 'NORM-04 rollback ABORTED: public.%.% no longer has the NORM-04 definition. Refusing to drop or rename it.',
                    r.child_table, r.agreed_name;
            END IF;
        END IF;

        IF r.action = 'created' THEN
            IF agreed_exists THEN
                EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I',
                               r.child_table, r.agreed_name);
                RAISE NOTICE 'NORM-04 rollback: dropped created constraint % on %',
                    r.agreed_name, r.child_table;
            END IF;

        ELSIF r.action = 'renamed' THEN
            SELECT EXISTS (
                SELECT 1
                  FROM pg_constraint c
                 WHERE c.conrelid = child_oid
                   AND c.conname  = r.original_name
            ) INTO original_exists;

            IF agreed_exists AND NOT original_exists THEN
                EXECUTE format('ALTER TABLE public.%I RENAME CONSTRAINT %I TO %I',
                               r.child_table, r.agreed_name, r.original_name);
                RAISE NOTICE 'NORM-04 rollback: restored % on % (renamed from %)',
                    r.original_name, r.child_table, r.agreed_name;
            ELSIF NOT agreed_exists AND original_exists THEN
                RAISE NOTICE 'NORM-04 rollback: % on % already restored',
                    r.original_name, r.child_table;
            ELSE
                RAISE EXCEPTION 'NORM-04 rollback ABORTED: cannot safely restore % on % (agreed % exists=%, original exists=%).',
                    r.original_name, r.child_table, r.agreed_name, agreed_exists, original_exists;
            END IF;

        ELSIF r.action = 'replaced' THEN
            IF r.original_definition IS NULL THEN
                RAISE EXCEPTION 'NORM-04 rollback ABORTED: no recorded definition for replaced constraint % on %.',
                    r.original_name, r.child_table;
            END IF;

            SELECT EXISTS (
                SELECT 1
                  FROM pg_constraint c
                 WHERE c.conrelid = child_oid
                   AND c.conname  = r.original_name
            ) INTO original_exists;

            IF agreed_exists THEN
                EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I',
                               r.child_table, r.agreed_name);
            END IF;

            IF NOT original_exists THEN
                EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I %s',
                               r.child_table, r.original_name,
                               r.original_definition);

                IF r.original_comment IS NOT NULL THEN
                    EXECUTE format('COMMENT ON CONSTRAINT %I ON public.%I IS %L',
                                   r.original_name, r.child_table,
                                   r.original_comment);
                END IF;

                RAISE NOTICE 'NORM-04 rollback: restored replaced constraint % on %',
                    r.original_name, r.child_table;
            ELSE
                RAISE NOTICE 'NORM-04 rollback: original constraint % on % already present',
                    r.original_name, r.child_table;
            END IF;
        END IF;
    END LOOP;

    -- ------------------------------------------------------------------
    -- Indexes
    -- ------------------------------------------------------------------
    FOR r IN
        SELECT *
          FROM public.database_migration_rollback_log
         WHERE object_kind = 'index'
         ORDER BY id
    LOOP
        child_oid := format('public.%I', r.child_table)::regclass;

        SELECT a.attnum INTO child_attnum
          FROM pg_attribute a
         WHERE a.attrelid = child_oid
           AND a.attname  = r.child_column
           AND NOT a.attisdropped;

        SELECT EXISTS (
            SELECT 1
              FROM pg_class ic
              JOIN pg_namespace n ON n.oid = ic.relnamespace
             WHERE n.nspname = 'public'
               AND ic.relname = r.agreed_name
               AND ic.relkind = 'i'
        ) INTO agreed_exists;

        IF agreed_exists THEN
            SELECT (i.indrelid      = child_oid
                    AND i.indnkeyatts = 1
                    AND i.indnatts    = 1
                    AND i.indkey[0]   = child_attnum
                    AND i.indpred     IS NULL
                    AND i.indexprs    IS NULL
                    AND i.indisvalid
                    AND NOT i.indisunique
                    AND am.amname = 'btree')
              INTO index_ok
              FROM pg_index i
              JOIN pg_class ic     ON ic.oid = i.indexrelid
              JOIN pg_namespace n  ON n.oid  = ic.relnamespace
              JOIN pg_am am        ON am.oid = ic.relam
             WHERE n.nspname = 'public'
               AND ic.relname = r.agreed_name;

            IF NOT COALESCE(index_ok, false) THEN
                RAISE EXCEPTION 'NORM-04 rollback ABORTED: index public.% no longer matches the NORM-04 definition. Refusing to drop it.',
                    r.agreed_name;
            END IF;

            EXECUTE format('DROP INDEX public.%I', r.agreed_name);
            RAISE NOTICE 'NORM-04 rollback: dropped created index %', r.agreed_name;
        END IF;
    END LOOP;

    DROP TABLE public.database_migration_rollback_log;
    RAISE NOTICE 'NORM-04 rollback: registry removed; FOR REVIEW lookup row and any pre-existing idx_proposals_status retained.';
END $norm04_rollback$;

COMMIT;

-- =============================================================================
-- VERIFICATION (read after COMMIT; expected 0 for every removed object)
-- =============================================================================
SELECT 'NORM-04 rollback registry remaining (expected 0)' AS check_name,
       count(*)::text AS value
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'public'
   AND c.relname = 'database_migration_rollback_log'
UNION ALL
SELECT 'NORM-04 named foreign keys remaining (expected 0)',
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
   AND c.conname IN ('fk_proposals_status', 'fk_psh_from_status', 'fk_psh_to_status')
UNION ALL
SELECT 'NORM-04 history indexes remaining (expected 0)',
       count(*)::text
  FROM pg_class ic
  JOIN pg_namespace n ON n.oid = ic.relnamespace
 WHERE n.nspname = 'public'
   AND ic.relkind = 'i'
   AND ic.relname IN ('idx_psh_from_status', 'idx_psh_to_status')
UNION ALL
SELECT 'pre-existing idx_proposals_status present (expected 1 on rpis_db)',
       count(*)::text
  FROM pg_class ic
  JOIN pg_namespace n ON n.oid = ic.relnamespace
 WHERE n.nspname = 'public'
   AND ic.relkind = 'i'
   AND ic.relname = 'idx_proposals_status'
UNION ALL
SELECT 'FOR REVIEW lookup row retained (expected 1)',
       count(*)::text
  FROM public.proposal_statuses
 WHERE code = 'FOR REVIEW';
