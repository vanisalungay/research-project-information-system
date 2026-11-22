-- =============================================================================
-- RPIS migration: role context on notifications
-- =============================================================================
-- A notification belonged only to the account, so a Proponent who was also a
-- Dean saw every role's notifications in both interfaces. This adds the role the
-- notification was raised for, which the API uses to filter the list, the unread
-- count and the read actions by the caller's current role.
--
-- Back-fill rules (only for rows that have no role yet):
--   1. an account that currently holds exactly one role -> that role;
--   2. Dean role appointment/revocation -> DEAN;
--   3. "New endorsement request" -> DEAN; an endorsement decision/letter to the
--      Proponent -> PROPONENT;
--   4. a workflow notification -> the recipient's reviewing-office role when the
--      account holds one (RPS admin/staff, OVCRIGE, REC, OVCAF, OC), otherwise
--      PROPONENT;
--   5. a Proponent-facing announcement/status -> PROPONENT;
--   6. anything that cannot be attributed safely stays NULL. Such a row is
--      served only to an account that holds a single role, never guessed into a
--      multi-role account's interface.
--
-- No existing row is deleted or duplicated; a row that already has a role is
-- left alone, so the migration is idempotent.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS recipient_role VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_notifications_user_role_created
    ON notifications (user_id, recipient_role, created_at DESC);

WITH roles AS (
    SELECT user_id,
           COUNT(*) AS role_count,
           MIN(role) AS only_role,
           MAX(CASE WHEN role = 'PROPONENT' THEN role END) AS proponent_role,
           MAX(CASE role
                   WHEN 'OC' THEN 1
                   WHEN 'OVCAF' THEN 2
                   WHEN 'REC' THEN 3
                   WHEN 'OVCRIGE' THEN 4
                   WHEN 'RPS_STAFF' THEN 5
                   WHEN 'RPS_ADMIN' THEN 6
               END) AS office_rank
      FROM user_roles
     GROUP BY user_id
), resolved AS (
    SELECT user_id,
           role_count,
           only_role,
           proponent_role,
           CASE office_rank
               WHEN 6 THEN 'RPS_ADMIN'
               WHEN 5 THEN 'RPS_STAFF'
               WHEN 4 THEN 'OVCRIGE'
               WHEN 3 THEN 'REC'
               WHEN 2 THEN 'OVCAF'
               WHEN 1 THEN 'OC'
           END AS office_role
      FROM roles
)
UPDATE notifications n
   SET recipient_role = CASE
       WHEN r.role_count = 1 THEN r.only_role
       WHEN n.type = 'DEAN_ASSIGNMENT' THEN 'DEAN'
       WHEN n.type = 'ENDORSEMENT'
            AND (n.title = 'New endorsement request'
                 OR n.message ILIKE '%waiting for your endorsement%') THEN 'DEAN'
       WHEN n.type = 'ENDORSEMENT'
            AND n.title IN ('Endorsement approved', 'Endorsement request rejected',
                            'Endorsement letter reissued') THEN 'PROPONENT'
       WHEN n.type = 'ENDORSEMENT' THEN COALESCE(r.office_role, r.proponent_role)
       WHEN n.type IN ('APPROVAL', 'REVIEW_REQUEST', 'REJECTION', 'PROPOSAL_UPDATE',
                       'REVISION', 'FUNDING', 'PROJECT_COMPLETION', 'QUARTERLY_REPORT')
            THEN COALESCE(r.office_role, r.proponent_role)
       WHEN n.type IN ('PROPOSAL_ANNOUNCEMENT', 'PROPOSAL_SUBMISSION',
                       'PROPOSAL_ENDORSED', 'REVISION_DEADLINE_REMINDER')
            THEN COALESCE(r.proponent_role, r.office_role)
       ELSE NULL
   END
  FROM resolved r
 WHERE n.user_id = r.user_id
   AND n.recipient_role IS NULL;

COMMIT;

-- ===========================================================================
-- VERIFICATION (read-only)
-- ===========================================================================
SELECT 'notifications with a role context' AS check_name, count(*)::text AS value
  FROM notifications
 WHERE recipient_role IS NOT NULL
UNION ALL
SELECT 'notifications without a role context',
       count(*)::text
  FROM notifications
 WHERE recipient_role IS NULL
UNION ALL
SELECT 'multi-role accounts with an unscoped notification',
       count(*)::text
  FROM notifications n
 WHERE n.recipient_role IS NULL
   AND (SELECT count(*) FROM user_roles r WHERE r.user_id = n.user_id) > 1
UNION ALL
SELECT 'notifications tagged with a role the account no longer holds',
       count(*)::text
  FROM notifications n
 WHERE n.recipient_role IS NOT NULL
   AND n.recipient_role <> 'DEAN'
   AND NOT EXISTS (
       SELECT 1 FROM user_roles r
        WHERE r.user_id = n.user_id AND r.role = n.recipient_role
   );
