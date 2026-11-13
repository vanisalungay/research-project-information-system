-- =============================================================================
-- RPIS business ERD - catalog snapshot (READ-ONLY)
-- =============================================================================
-- Writes docs/erd/rpis-business-erd.json containing:
--   * the 30 business tables in public (everything except the NORM-04
--     rollback registry, which is migration infrastructure)
--   * every column, its exact type, nullability, default, PK and FK membership
--   * every foreign key, its child/parent tables and columns, and actions
--
-- Run:
--   psql -h localhost -U postgres -d rpis_db -v ON_ERROR_STOP=1 -f extract-erd-data.sql
--
-- This script performs no DDL or DML.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN TRANSACTION READ ONLY;
\pset pager off
\pset format unaligned
\pset tuples_only on
\o 'C:/Users/ACER/Documents/ccc181/research-project-information-system/docs/erd/rpis-business-erd.json'

SELECT jsonb_pretty(jsonb_build_object(
    'database',      current_database(),
    'schema',        'public',
    'generated_at',  to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS'),
    'excluded_tables', json_build_array('database_migration_rollback_log'),
    'tables', (
        SELECT json_agg(tbl ORDER BY tbl->>'name')
          FROM (
            SELECT json_build_object(
                'name', c.relname,
                'columns', (
                    SELECT json_agg(json_build_object(
                               'name', a.attname,
                               'type', format_type(a.atttypid, a.atttypmod),
                               'nullable', NOT a.attnotnull,
                               'default', pg_get_expr(ad.adbin, ad.adrelid),
                               'is_primary_key', EXISTS (
                                   SELECT 1 FROM pg_constraint pk
                                    WHERE pk.conrelid = c.oid
                                      AND pk.contype = 'p'
                                      AND a.attnum = ANY (pk.conkey)),
                               'is_foreign_key', EXISTS (
                                   SELECT 1 FROM pg_constraint fk
                                    WHERE fk.conrelid = c.oid
                                      AND fk.contype = 'f'
                                      AND a.attnum = ANY (fk.conkey))
                           ) ORDER BY a.attnum)
                      FROM pg_attribute a
                      LEFT JOIN pg_attrdef ad
                             ON ad.adrelid = a.attrelid AND ad.adnum = a.attnum
                     WHERE a.attrelid = c.oid
                       AND a.attnum > 0
                       AND NOT a.attisdropped
                )
            ) AS tbl
              FROM pg_class c
              JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE n.nspname = 'public'
               AND c.relkind = 'r'
               AND c.relname <> 'database_migration_rollback_log'
          ) s
    ),
    'foreign_keys', (
        SELECT json_agg(fk ORDER BY fk->>'child_table', fk->>'constraint_name')
          FROM (
            SELECT json_build_object(
                'constraint_name', con.conname,
                'child_table',     con.conrelid::regclass::text,
                'child_columns', (
                    SELECT json_agg(a.attname ORDER BY k.ord)
                      FROM unnest(con.conkey) WITH ORDINALITY k(attnum, ord)
                      JOIN pg_attribute a
                        ON a.attrelid = con.conrelid AND a.attnum = k.attnum),
                'parent_table',    con.confrelid::regclass::text,
                'parent_columns', (
                    SELECT json_agg(a.attname ORDER BY k.ord)
                      FROM unnest(con.confkey) WITH ORDINALITY k(attnum, ord)
                      JOIN pg_attribute a
                        ON a.attrelid = con.confrelid AND a.attnum = k.attnum),
                'on_delete', CASE con.confdeltype
                                  WHEN 'a' THEN 'NO ACTION'
                                  WHEN 'r' THEN 'RESTRICT'
                                  WHEN 'c' THEN 'CASCADE'
                                  WHEN 'n' THEN 'SET NULL'
                                  WHEN 'd' THEN 'SET DEFAULT'
                                  ELSE con.confdeltype::text END,
                'on_update', CASE con.confupdtype
                                  WHEN 'a' THEN 'NO ACTION'
                                  WHEN 'r' THEN 'RESTRICT'
                                  WHEN 'c' THEN 'CASCADE'
                                  WHEN 'n' THEN 'SET NULL'
                                  WHEN 'd' THEN 'SET DEFAULT'
                                  ELSE con.confupdtype::text END,
                'validated', con.convalidated
            ) AS fk
              FROM pg_constraint con
              JOIN pg_namespace n ON n.oid = con.connamespace
             WHERE n.nspname = 'public'
               AND con.contype = 'f'
               AND con.conrelid <> 'public.database_migration_rollback_log'::regclass
               AND con.confrelid <> 'public.database_migration_rollback_log'::regclass
          ) s
    ),
    'counts', json_build_object(
        'business_tables', (
            SELECT count(*) FROM pg_class c
              JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE n.nspname = 'public' AND c.relkind = 'r'
               AND c.relname <> 'database_migration_rollback_log'),
        'foreign_keys', (
            SELECT count(*) FROM pg_constraint con
              JOIN pg_namespace n ON n.oid = con.connamespace
             WHERE n.nspname = 'public' AND con.contype = 'f'
               AND con.conrelid <> 'public.database_migration_rollback_log'::regclass
               AND con.confrelid <> 'public.database_migration_rollback_log'::regclass)
    )
));

\o
ROLLBACK;
