-- =============================================================================
-- RPIS migration: correct the institutional e-mail domain
-- =============================================================================
-- Account creation used the misspelled domain @msunawaan.edu.ph instead of the
-- institution's real domain @msunaawan.edu.ph. The code is corrected; this
-- migration corrects the accounts that were created with the misspelling.
--
-- Rules:
--   * only the domain is replaced; the local part is kept exactly as stored;
--   * a row is left untouched when the corrected address already belongs to
--     another account, so the unique address constraint is never violated and
--     no two accounts are merged. The verification queries below list those
--     conflicts for manual resolution;
--   * the Dean appointment snapshots are updated only through their account, and
--     only when the local part agrees, so a historical row is never pointed at a
--     different person;
--   * ids, roles, passwords, names, proposals, endorsements and audit rows are
--     not touched.
--
-- Idempotent: after the correction no row matches the retired domain.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

-- 1. Accounts: replace only the domain, and only when the corrected address is free.
UPDATE users u
   SET email = split_part(u.email, '@', 1) || '@msunaawan.edu.ph'
 WHERE LOWER(u.email) LIKE '%@msunawaan.edu.ph'
   AND NOT EXISTS (
       SELECT 1
         FROM users other
        WHERE LOWER(other.email) =
              LOWER(split_part(u.email, '@', 1) || '@msunaawan.edu.ph')
   );

-- 2. Appointment snapshots: follow the corrected account. The local part must
--    still agree, so a snapshot is never re-pointed to another person.
UPDATE dean_assignments a
   SET dean_email = u.email
  FROM users u
 WHERE u.id = a.user_id
   AND LOWER(a.dean_email) LIKE '%@msunawaan.edu.ph'
   AND LOWER(u.email) LIKE '%@msunaawan.edu.ph'
   AND LOWER(split_part(a.dean_email, '@', 1)) =
       LOWER(split_part(u.email, '@', 1));

COMMIT;

-- ===========================================================================
-- VERIFICATION (read-only)
-- ===========================================================================
SELECT 'accounts on the correct domain' AS check_name, count(*)::text AS value
  FROM users
 WHERE LOWER(email) LIKE '%@msunaawan.edu.ph'
UNION ALL
SELECT 'accounts still on the retired domain',
       count(*)::text
  FROM users
 WHERE LOWER(email) LIKE '%@msunawaan.edu.ph'
UNION ALL
SELECT 'accounts with an unresolved conflict',
       count(*)::text
  FROM users u
 WHERE LOWER(u.email) LIKE '%@msunawaan.edu.ph'
   AND EXISTS (
       SELECT 1 FROM users other
        WHERE LOWER(other.email) =
              LOWER(split_part(u.email, '@', 1) || '@msunaawan.edu.ph')
   )
UNION ALL
SELECT 'appointment snapshots still on the retired domain',
       count(*)::text
  FROM dean_assignments
 WHERE LOWER(dean_email) LIKE '%@msunawaan.edu.ph';
