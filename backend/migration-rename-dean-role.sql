-- Migration: rename the Dean role value  RPS_DEAN -> DEAN
-- ---------------------------------------------------------------------------
-- The Dean / Department Chair is an independent role, not part of RPS, so the
-- application now stores, transmits and compares `DEAN` (the canonical vocabulary is
-- backend/src/main/java/com/rpis/backend/model/UserRoles.java, and the Spring Security
-- rules reference the same constant).
--
-- A constant rename changes no row, and the role value is stored verbatim in
-- `users.role`, embedded in the bearer token and compared by SecurityConfig. A database
-- that still holds RPS_DEAN therefore cannot authenticate or authorize a Dean:
--   * POST /api/users/login compares the submitted role with the stored one, so a Dean
--     signing in as DEAN is answered 401;
--   * the token filter grants ROLE_<stored value>, so ROLE_RPS_DEAN no longer satisfies
--     hasRole(DEAN) and PUT /api/endorsements/{id}/approve|reject answers 403.
-- Run this migration with the deployment, before the new build serves traffic.
--
-- Characteristics:
--   * Idempotent       -> every statement matches only the retired value, so re-running
--                         it (or running it on an already-migrated database) is a no-op.
--   * Non-destructive  -> no row, table, column, index or constraint is dropped; the
--                         only change is the spelling of one role value.
--   * No DDL           -> `users.role` and the audit role columns are free text
--                         (VARCHAR, NOT NULL for users.role) with no CHECK constraint,
--                         enum or trigger, so a plain UPDATE is sufficient and no
--                         constraint has to be dropped or recreated.
--
-- Order: run this after the scripts that create the audit tables it touches
-- (migration-add-endorsement-workflow.sql, migration-add-proposal-announcement-updates.sql,
-- migration-add-proposal-section-remarks.sql), i.e. last in the list in DEPLOYMENT.md
-- section 4.
--
-- Apply with (safe inside a transaction - all statements are data updates):
--   BEGIN;
--   \i migration-rename-dean-role.sql
--   COMMIT;

-- The account's role. This is the row that decides what the account may do.
UPDATE users
   SET role = 'DEAN'
 WHERE role = 'RPS_DEAN';

-- Audit and history snapshots of the role. No authorization decision reads them, but
-- they are displayed as-is (the Dean's request timeline renders `actor_role`), so a
-- stale value would be visible to users describing the role that was just renamed.
UPDATE endorsement_request_events
   SET actor_role = 'DEAN'
 WHERE actor_role = 'RPS_DEAN';

UPDATE proposal_announcement_updates
   SET actor_role = 'DEAN'
 WHERE actor_role = 'RPS_DEAN';

UPDATE proposal_reviews
   SET reviewer_role = 'DEAN'
 WHERE reviewer_role = 'RPS_DEAN';

UPDATE proposal_section_remarks
   SET reviewer_role = 'DEAN'
 WHERE reviewer_role = 'RPS_DEAN';

-- Verification: every count must be 0 after the migration. This is the same check the
-- automated guard (DeanRoleValueCountTest) performs.
SELECT 'users.role' AS role_column, count(*) AS retired_values
  FROM users WHERE role = 'RPS_DEAN'
UNION ALL
SELECT 'endorsement_request_events.actor_role', count(*)
  FROM endorsement_request_events WHERE actor_role = 'RPS_DEAN'
UNION ALL
SELECT 'proposal_announcement_updates.actor_role', count(*)
  FROM proposal_announcement_updates WHERE actor_role = 'RPS_DEAN'
UNION ALL
SELECT 'proposal_reviews.reviewer_role', count(*)
  FROM proposal_reviews WHERE reviewer_role = 'RPS_DEAN'
UNION ALL
SELECT 'proposal_section_remarks.reviewer_role', count(*)
  FROM proposal_section_remarks WHERE reviewer_role = 'RPS_DEAN';
