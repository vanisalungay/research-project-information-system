-- Migration: link a notification to the Proposal Announcement (Call for Proposals)
-- it was raised for.
-- ---------------------------------------------------------------------------
-- Proponents receive an in-app notification when a Call for Proposals is
-- published or updated. Without a reference to the announcement, clicking that
-- notification could only change its read state - there was nothing to open.
--
--   proposal_announcement_id   The proposal_announcements.id the notification is
--                              about. NULL for every notification that is not
--                              about an announcement (for example proposal
--                              status notifications), which therefore simply has
--                              no announcement to open.
--
-- Safe to run on existing databases: the change is additive and nullable, so all
-- existing notifications keep their behaviour (they have no announcement to
-- open). Only notifications created from now on carry the reference.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate also adds this column
-- automatically; this script documents/keeps the DDL explicit.

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS proposal_announcement_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_notifications_proposal_announcement_id
    ON notifications (proposal_announcement_id);
