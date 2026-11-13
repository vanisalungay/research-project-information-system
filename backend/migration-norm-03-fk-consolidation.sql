-- =============================================================================
-- RPIS Migration NORM-03: foreign-key consolidation - rpis_db
-- =============================================================================
-- ONE relationship = ONE foreign key.
--
-- WHY: migration-add-quarterly-progress-reports.sql creates its foreign keys
-- without names, so PostgreSQL generates <table>_<column>_fkey. The FK blocks
-- in migration-01 and migration-norm-01 originally matched by constraint NAME
-- only, did not see those anonymous constraints, and added a second, named
-- constraint for the same relationship. The live catalog had three such pairs:
--
--   quarterly_progress_reports.proposal_id
--       fk_qpr_proposal                       ON DELETE RESTRICT
--       quarterly_progress_reports_proposal_id_fkey  ON DELETE CASCADE
--   quarterly_progress_reports.created_by
--       fk_qpr_created_by                     ON DELETE SET NULL
--       quarterly_progress_reports_created_by_fkey   ON DELETE SET NULL
--   quarterly_progress_reports.submitted_by
--       fk_qpr_submitted_by                   ON DELETE SET NULL
--       quarterly_progress_reports_submitted_by_fkey ON DELETE SET NULL
--
-- The guards in migration-01 and migration-norm-01 now detect a relationship
-- by (child table, child column -> parent table, id), so they no longer create
-- duplicates. This migration converges a database that already has them.
--
-- RULES (per relationship, idempotent):
--   * no FK yet           -> prove 0 orphans, add the named FK
--   * one FK, other name  -> rename it to the agreed name
--   * several FKs         -> keep the agreed name (else the agreed delete
--                            action), drop the rest
--   * survivor's action differs from the agreed action -> rebuild it
--
-- AGREED DELETE ACTIONS:
--   quarterly_progress_reports.proposal_id -> proposals              RESTRICT
--        (explicit decision: progress reports must not be silently erased
--         with their proposal; this was also the effective live behaviour)
--   quarterly_progress_reports.created_by / submitted_by -> users    SET NULL
--   notifications.proposal_announcement_id -> proposal_announcements SET NULL
--
-- NEW RELATIONSHIP: notifications.proposal_announcement_id had no FK. The
-- column was verified to hold 0 orphan rows, so the constraint is added.
-- notifications.proposal_id is NOT touched: it currently has 3 orphan rows
-- (proposal 18 no longer exists), which is a data defect to be resolved
-- before that FK can be added. endorsement_request_events.actor_id stays
-- excluded by migration-01 (historical actor id 79 must be preserved).
--
-- RE-RUNNING: a second run is a no-op; every relationship already matches.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $norm03_fks$
DECLARE
    e          record;
    eq_count   int;
    kept_name  text;
    kept_type  text;
    n          bigint;
    victim     record;
BEGIN
    FOR e IN
        SELECT * FROM (VALUES
            ('quarterly_progress_reports', 'proposal_id',             'proposals',             'fk_qpr_proposal',                        'r', 'RESTRICT'),
            ('quarterly_progress_reports', 'created_by',              'users',                 'fk_qpr_created_by',                      'n', 'SET NULL'),
            ('quarterly_progress_reports', 'submitted_by',            'users',                 'fk_qpr_submitted_by',                    'n', 'SET NULL'),
            ('notifications',              'proposal_announcement_id', 'proposal_announcements', 'fk_notifications_proposal_announcement', 'n', 'SET NULL')
        ) AS t(child_table, child_col, parent_table, conname, confdeltype, on_delete)
    LOOP
        -- Existing single-column FKs for this exact relationship. The survivor
        -- is chosen by: agreed name first, then agreed delete action, then name.
        SELECT count(*)::int,
               (array_agg(c.conname
                  ORDER BY (c.conname = e.conname) DESC,
                           (c.confdeltype::text = e.confdeltype) DESC,
                           c.conname))[1],
               (array_agg(c.confdeltype::text
                  ORDER BY (c.conname = e.conname) DESC,
                           (c.confdeltype::text = e.confdeltype) DESC,
                           c.conname))[1]
          INTO eq_count, kept_name, kept_type
          FROM pg_constraint c
          JOIN pg_class     t  ON t.oid = c.conrelid
          JOIN pg_namespace ns ON ns.oid = t.relnamespace
          JOIN pg_class     pt ON pt.oid = c.confrelid
         WHERE c.contype = 'f'
           AND ns.nspname = 'public'
           AND t.relname  = e.child_table
           AND pt.relname = e.parent_table
           AND array_length(c.conkey, 1) = 1
           AND c.conkey[1]  = (SELECT a.attnum FROM pg_attribute a
                                WHERE a.attrelid = c.conrelid
                                  AND a.attname  = e.child_col)
           AND c.confkey[1] = (SELECT a.attnum FROM pg_attribute a
                                WHERE a.attrelid = c.confrelid
                                  AND a.attname  = 'id');

        IF eq_count = 0 THEN
            EXECUTE format('SELECT count(*) FROM public.%I c WHERE c.%I IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.%I p WHERE p.id = c.%I)',
                           e.child_table, e.child_col, e.parent_table, e.child_col) INTO n;
            IF n > 0 THEN
                RAISE EXCEPTION 'NORM-03: % orphan row(s) in %.% - cannot add %',
                    n, e.child_table, e.child_col, e.conname;
            END IF;
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.%I(id) ON DELETE %s ON UPDATE NO ACTION',
                           e.child_table, e.conname, e.child_col, e.parent_table, e.on_delete);
            RAISE NOTICE 'NORM-03: created % (%.% -> %)',
                e.conname, e.child_table, e.child_col, e.parent_table;

        ELSE
            -- Drop every equivalent FK except the survivor.
            FOR victim IN
                SELECT c2.conname
                  FROM pg_constraint c2
                  JOIN pg_class     t  ON t.oid = c2.conrelid
                  JOIN pg_namespace ns ON ns.oid = t.relnamespace
                  JOIN pg_class     pt ON pt.oid = c2.confrelid
                 WHERE c2.contype = 'f'
                   AND ns.nspname = 'public'
                   AND t.relname  = e.child_table
                   AND pt.relname = e.parent_table
                   AND array_length(c2.conkey, 1) = 1
                   AND c2.conkey[1]  = (SELECT a.attnum FROM pg_attribute a
                                         WHERE a.attrelid = c2.conrelid
                                           AND a.attname  = e.child_col)
                   AND c2.confkey[1] = (SELECT a.attnum FROM pg_attribute a
                                         WHERE a.attrelid = c2.confrelid
                                           AND a.attname  = 'id')
                   AND c2.conname <> kept_name
            LOOP
                EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I',
                               e.child_table, victim.conname);
                RAISE NOTICE 'NORM-03: dropped redundant % (kept %)', victim.conname, kept_name;
            END LOOP;

            IF kept_type <> e.confdeltype THEN
                -- Correct relationship, wrong delete action: rebuild it.
                EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I',
                               e.child_table, kept_name);
                EXECUTE format('SELECT count(*) FROM public.%I c WHERE c.%I IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.%I p WHERE p.id = c.%I)',
                               e.child_table, e.child_col, e.parent_table, e.child_col) INTO n;
                IF n > 0 THEN
                    RAISE EXCEPTION 'NORM-03: % orphan row(s) in %.% - cannot recreate %',
                        n, e.child_table, e.child_col, e.conname;
                END IF;
                EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.%I(id) ON DELETE %s ON UPDATE NO ACTION',
                               e.child_table, e.conname, e.child_col, e.parent_table, e.on_delete);
                RAISE NOTICE 'NORM-03: rebuilt % with ON DELETE %', e.conname, e.on_delete;

            ELSIF kept_name <> e.conname THEN
                EXECUTE format('ALTER TABLE public.%I RENAME CONSTRAINT %I TO %I',
                               e.child_table, kept_name, e.conname);
                RAISE NOTICE 'NORM-03: renamed % to %', kept_name, e.conname;
            END IF;
        END IF;
    END LOOP;
END $norm03_fks$;

COMMIT;

-- =============================================================================
-- VERIFICATION (read after COMMIT)
-- =============================================================================
-- 1) The four consolidated relationships, exactly one constraint each.
SELECT t.relname             AS child_table,
       a.attname             AS child_column,
       pt.relname            AS parent_table,
       c.conname             AS constraint_name,
       c.confdeltype::text   AS on_delete,
       c.convalidated        AS validated
  FROM pg_constraint c
  JOIN pg_class     t  ON t.oid = c.conrelid
  JOIN pg_namespace ns ON ns.oid = t.relnamespace
  JOIN pg_class     pt ON pt.oid = c.confrelid
  JOIN pg_attribute a  ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
 WHERE c.contype = 'f'
   AND ns.nspname = 'public'
   AND (t.relname, a.attname) IN (
        ('quarterly_progress_reports', 'proposal_id'),
        ('quarterly_progress_reports', 'created_by'),
        ('quarterly_progress_reports', 'submitted_by'),
        ('notifications',              'proposal_announcement_id'))
 ORDER BY t.relname, a.attname;

-- 2) No relationship anywhere in public is represented by more than one FK.
--    Expect 0.
SELECT 'duplicate FK relationships remaining (expected 0)' AS check_name,
       count(*)::text AS value
  FROM (
    SELECT c.conrelid, c.conkey, c.confrelid, c.confkey
      FROM pg_constraint c
     WHERE c.contype = 'f'
       AND c.connamespace = 'public'::regnamespace
     GROUP BY c.conrelid, c.conkey, c.confrelid, c.confkey
    HAVING count(*) > 1
  ) d
UNION ALL
SELECT 'public foreign keys (informational)',
       count(*)::text
  FROM pg_constraint
 WHERE contype = 'f' AND connamespace = 'public'::regnamespace;
