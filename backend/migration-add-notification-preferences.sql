-- Migration: Notification Routing preferences on the account
-- ---------------------------------------------------------------------------
-- Manage Profile -> Notifications lets a user choose which proposal-status and
-- deadline alerts they want. Those choices are now stored on the account instead
-- of living only in the page: the page previously rendered hardcoded option lists
-- whose "Save Notification Settings" button only displayed a confirmation, so
-- nothing was persisted and the selection was lost on reload.
--
-- Characteristics:
--   * Additive and nullable  -> existing accounts simply have no preferences yet,
--                               which reads as "nothing selected".
--   * Idempotent             -> ADD COLUMN IF NOT EXISTS; safe to re-run.
--   * Non-destructive        -> no table, row or constraint is dropped or changed.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update (the dev profile) Hibernate also
-- adds these columns automatically; this script documents/keeps the DDL explicit
-- and is what a real database must be provisioned with.
--
-- Apply with:
--   psql -h localhost -U postgres -d <database> -f migration-add-notification-preferences.sql

ALTER TABLE users ADD COLUMN IF NOT EXISTS notify_proposals TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notify_deadlines TEXT;
