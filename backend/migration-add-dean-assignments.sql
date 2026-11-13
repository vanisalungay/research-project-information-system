-- Migration: Dean appointment history (Dean succession)
-- ---------------------------------------------------------------------------
-- A Dean / Department Chair is appointed to one college or department. Until now the
-- appointment lived only on the account (users.assigned_college, users.designation), so
-- replacing a Dean overwrote those columns and the previous appointment was lost. This
-- migration introduces `dean_assignments`, where every appointment is its own row: the
-- sitting Dean is the row with status = 'ACTIVE', and every predecessor is retained with
-- status = 'REVOKED', its date_revoked and the administrator (revoked_by) who replaced it.
--
-- Existing data is preserved: the current Dean of every college is backfilled as one
-- ACTIVE assignment, so no current assignment is lost when the new structure takes over.
-- No table, row or column is dropped or changed, and no account is deleted.
--
-- Characteristics:
--   * Additive             -> creates one new table and its indexes only.
--   * Idempotent           -> CREATE ... IF NOT EXISTS; the backfill inserts only when the
--                            account has no ACTIVE assignment yet, so re-running is a no-op.
--   * Non-destructive      -> users.assigned_college / users.designation are left in place
--                            and keep meaning "the current scope of the sitting Dean".
--
-- The partial unique index is the database-level guarantee of the business rule "only one
-- active Dean per college": it makes a second ACTIVE row for the same college impossible,
-- even if two administrators race. The service also enforces it inside the replacement
-- transaction; this index is the backstop.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update (the dev profile) Hibernate also creates
-- this table from the DeanAssignment entity; this script documents/keeps the DDL explicit
-- and is what a real database must be provisioned with (ddl-auto=validate in prod).
--
-- Apply with:
--   psql -h localhost -U postgres -d <database> -f migration-add-dean-assignments.sql

CREATE TABLE IF NOT EXISTS dean_assignments (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    dean_name       VARCHAR(255),
    dean_email      VARCHAR(255),
    assigned_college VARCHAR(255),
    designation     VARCHAR(255),
    status          VARCHAR(20)  NOT NULL,
    date_assigned   TIMESTAMP    NOT NULL,
    date_revoked    TIMESTAMP,
    created_by      BIGINT,
    created_by_name VARCHAR(255),
    revoked_by      BIGINT,
    revoked_by_name VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_dean_assignments_user   ON dean_assignments (user_id);
CREATE INDEX IF NOT EXISTS idx_dean_assignments_status ON dean_assignments (status);

-- At most one ACTIVE appointment per college (NULL college collapses to '' so the
-- institution-wide fallback is covered too).
CREATE UNIQUE INDEX IF NOT EXISTS ux_dean_assignments_active_college
    ON dean_assignments (COALESCE(assigned_college, ''))
    WHERE status = 'ACTIVE';

-- Backfill: every account that currently holds the Dean role becomes its college's ACTIVE
-- appointment. Guarded so it runs once and never duplicates an existing active row.
INSERT INTO dean_assignments
    (user_id, dean_name, dean_email, assigned_college, designation, status,
     date_assigned, created_by_name)
SELECT u.id, u.name, u.email, u.assigned_college, u.designation, 'ACTIVE',
       COALESCE(u.date_registered::timestamp, now()), 'Migration backfill'
  FROM users u
 WHERE u.role = 'DEAN'
   AND NOT EXISTS (
       SELECT 1 FROM dean_assignments a
        WHERE a.user_id = u.id AND a.status = 'ACTIVE'
   );

-- Verification: one ACTIVE appointment per sitting (DEAN) account, none missing.
SELECT 'active_dean_assignments' AS what, count(*) AS n
  FROM dean_assignments WHERE status = 'ACTIVE'
UNION ALL
SELECT 'deans_without_active_assignment', count(*)
  FROM users u
 WHERE u.role = 'DEAN'
   AND NOT EXISTS (
       SELECT 1 FROM dean_assignments a
        WHERE a.user_id = u.id AND a.status = 'ACTIVE'
   );
