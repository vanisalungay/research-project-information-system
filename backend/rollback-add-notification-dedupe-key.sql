-- Rollback for migration-add-notification-dedupe-key.sql
DROP INDEX IF EXISTS ux_notifications_user_dedupe;
ALTER TABLE notifications
    DROP COLUMN IF EXISTS dedupe_key;
