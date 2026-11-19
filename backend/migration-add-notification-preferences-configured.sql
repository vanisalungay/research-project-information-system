-- Migration: distinguish "never configured" from "all optional alerts off"
-- ---------------------------------------------------------------------------
-- The Notifications tab stores the selected option keys in users.notify_proposals
-- and users.notify_deadlines as JSON arrays. Before this flag existed, a never-opened
-- tab and a deliberately cleared tab both stored an empty array, so a notification
-- generator could not tell which one it was.
--
--   FALSE (default)  no explicit choice; optional proposal alerts use the defaults,
--                    deadline reminders stay off (opt-in).
--   TRUE             the account saved the tab; an empty array is a real all-off
--                    choice for that group.
--
-- Additive and idempotent. Existing rows keep their current arrays; code treats a
-- legacy non-empty array as configured without rewriting it.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS notify_preferences_configured BOOLEAN NOT NULL DEFAULT FALSE;
