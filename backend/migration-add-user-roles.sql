-- =============================================================================
-- RPIS migration: allow one account to hold several roles
-- =============================================================================
-- users.role used to be the only role an account could have, so appointing an
-- existing Proponent as Dean had to create a second account with a numbered
-- address (vanisa.lungay2@...). That duplicated the person. This migration adds
-- the assignment table the account now reads:
--
--   * users.role stays the account's primary role (the role it was created with);
--   * user_roles lists every role the account may sign in as;
--   * the existing value is back-filled once, so every current account keeps
--     exactly the role it had and no single-role account changes behaviour.
--
-- Additive and idempotent: the table and index are created only when missing, and
-- the back-fill inserts only the rows that are not already there. No user row is
-- updated and no account, appointment or endorsement record is removed.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Every existing account keeps the role it already had. The value is normalized the
-- same way the application normalizes it (trimmed, upper case) so the Java side's
-- case-insensitive comparison and this table agree.
INSERT INTO user_roles (user_id, role)
SELECT id, UPPER(TRIM(role))
  FROM users
 WHERE role IS NOT NULL
   AND TRIM(role) <> ''
ON CONFLICT (user_id, role) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles (role);

COMMIT;

-- ===========================================================================
-- VERIFICATION (read-only)
-- ===========================================================================
SELECT 'user role assignments' AS check_name, count(*)::text AS value
  FROM user_roles
UNION ALL
SELECT 'accounts with no role assignment',
       count(*)::text
  FROM users u
 WHERE NOT EXISTS (SELECT 1 FROM user_roles r WHERE r.user_id = u.id)
UNION ALL
SELECT 'accounts holding more than one role',
       count(*)::text
  FROM (SELECT user_id FROM user_roles GROUP BY user_id HAVING count(*) > 1) multi;
