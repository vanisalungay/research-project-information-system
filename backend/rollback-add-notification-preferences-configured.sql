-- Rollback for migration-add-notification-preferences-configured.sql
-- The column is additive and safe to leave in place. Dropping it removes the
-- never-configured/all-clear distinction and returns every account to the default
-- behaviour.
ALTER TABLE users
    DROP COLUMN IF EXISTS notify_preferences_configured;
