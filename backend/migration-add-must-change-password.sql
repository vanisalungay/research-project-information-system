-- Migration: first-login password change flag on the account
-- ---------------------------------------------------------------------------
-- An account an administrator creates is issued an institutional address and a securely
-- generated temporary password. The account must replace that password before it can use the
-- rest of the application, so the account carries a flag saying so: the login response
-- reports it, the client sends the user to the change-password page, and the API refuses the
-- account until the password is changed.
--
-- Characteristics:
--   * Additive and defaulted -> every existing account reads as FALSE, so no current user is
--                               asked to change a password they chose themselves.
--   * Idempotent             -> ADD COLUMN IF NOT EXISTS; safe to re-run.
--   * Non-destructive        -> no table, row or constraint is dropped or changed.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update (the dev profile) Hibernate also adds this
-- column from the entity; this script documents/keeps the DDL explicit and is what a real
-- database must be provisioned with.
--
-- Apply with:
--   psql -h localhost -U postgres -d <database> -f migration-add-must-change-password.sql

ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE;
UPDATE users SET must_change_password = FALSE WHERE must_change_password IS NULL;