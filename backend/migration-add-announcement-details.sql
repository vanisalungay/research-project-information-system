-- Migration: Proposal Announcement details (RIIDE priority agenda, standard
-- reminders, additional announcement text)
-- ---------------------------------------------------------------------------
-- Adds the columns that let OVCRIGE prepare a complete Call for Proposals
-- announcement from RPIS:
--
--   priority_agendas          JSON array of the selected MSUN RIIDE 2025-2028
--                             program keys, e.g. ["dagat","punla"]. Stable
--                             identifiers - never display labels or positions.
--   reminder_ids              JSON array of the selected standard reminder ids,
--                             e.g. ["research-plan","eligible-submitters",
--                             "collaboration"].
--   additional_announcement   Optional free-text announcement body (plain text,
--                             line breaks preserved).
--
-- Safe to run on existing databases: the change is additive, existing
-- announcements keep their name/dates/status and simply carry NULL for the new
-- values (no agenda, no reminder, no extra text), so they stay readable and
-- editable.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate also adds these
-- columns automatically; this script documents/keeps the DDL explicit.
ALTER TABLE proposal_announcements
    ADD COLUMN IF NOT EXISTS priority_agendas TEXT;

ALTER TABLE proposal_announcements
    ADD COLUMN IF NOT EXISTS reminder_ids TEXT;

ALTER TABLE proposal_announcements
    ADD COLUMN IF NOT EXISTS additional_announcement TEXT;
