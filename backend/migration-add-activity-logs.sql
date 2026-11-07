-- ===========================================================================
-- Activity Log: the per-administrator activity history behind the RPS ADMIN
-- "Activity Log" page.
-- ===========================================================================
-- Additive and idempotent, like every other migration-*.sql here: the table is
-- created when it does not exist and no existing row is dropped, rewritten or
-- back-filled. Accounts, proposals, calls for proposal and notifications are
-- untouched.
--
-- Rows describe actions that happen from now on. Nothing is invented for
-- operations performed before this migration was applied.
--
-- The table is append-only: the application only ever INSERTs into it, and no
-- API route updates or deletes an activity record.
--
--   psql -h localhost -U postgres -d rpis_db -f backend/migration-add-activity-logs.sql
-- ===========================================================================

CREATE TABLE IF NOT EXISTS activity_logs (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    user_name   VARCHAR(255),
    action      VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    module      VARCHAR(255) NOT NULL,
    details     TEXT,
    status      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP
);

-- The page always reads one administrator's rows, newest first, and the module
-- filter groups them, so both are indexed.
CREATE INDEX IF NOT EXISTS idx_activity_logs_user_created ON activity_logs (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_activity_logs_module ON activity_logs (module);
