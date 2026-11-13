-- Migration: server-side invalidation of bearer tokens on sign-out
-- ---------------------------------------------------------------------------
-- A token used to be valid until it expired: signing out only deleted it from the
-- browser, so a copy of the token kept working for the rest of its lifetime. The
-- account now carries a generation number, embedded in every token it is issued.
-- Signing out increments the number, and the authentication filter accepts a token
-- only while its embedded number equals the stored one.
--
-- The counter lives in the shared database, so the invalidation is seen by every
-- instance behind the load balancer - not only the one that served the sign-out.
--
-- Characteristics:
--   * Additive and defaulted -> every existing account is generation 0, which is also
--                               how a token issued before this change is read. No
--                               current user is signed out by applying the migration.
--   * Idempotent             -> guarded ADD COLUMN / UPDATE / SET DEFAULT / SET NOT NULL;
--                               safe to re-run.
--   * Non-destructive        -> no table, row or constraint is dropped or changed.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update (the loadtest profile) Hibernate
-- also adds this column from the entity; this script is what every real database
-- (ddl-auto=validate) is provisioned with.
--
-- Apply with:
--   psql -h localhost -U postgres -d <database> -f migration-add-token-version.sql

ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INTEGER;
UPDATE users SET token_version = 0 WHERE token_version IS NULL;
ALTER TABLE users ALTER COLUMN token_version SET DEFAULT 0;
ALTER TABLE users ALTER COLUMN token_version SET NOT NULL;
