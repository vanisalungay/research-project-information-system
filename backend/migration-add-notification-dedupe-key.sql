-- Migration: idempotency key for generated notifications
-- ---------------------------------------------------------------------------
-- Revision-deadline reminders are produced by a scheduled job. The job is already
-- serialized across instances with a PostgreSQL advisory lock, but a unique key makes
-- the guarantee durable even if a lock is lost or a retry runs twice.
--
--   dedupe_key NULL      ordinary notification; the partial unique index ignores it.
--   dedupe_key value     one row per recipient for that generated event.
--
-- Additive and idempotent. No existing row is changed.
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(200);

CREATE UNIQUE INDEX IF NOT EXISTS ux_notifications_user_dedupe
    ON notifications (user_id, dedupe_key)
    WHERE dedupe_key IS NOT NULL;
