-- Migration: rename application_cycles -> proposal_announcements
-- ---------------------------------------------------------------------------
-- Renames the existing "application_cycles" table, its indexes, and its
-- constraints to "proposal_announcements" so existing data is preserved after
-- the entity/table were renamed in code (the ProposalAnnouncement model maps to
-- table "proposal_announcements").
--
-- IMPORTANT: run this BEFORE restarting the backend with the renamed entity.
-- With spring.jpa.hibernate.ddl-auto=update, Hibernate would otherwise create a
-- brand-new, empty "proposal_announcements" table and leave this one orphaned.

ALTER TABLE IF EXISTS application_cycles RENAME TO proposal_announcements;

ALTER INDEX IF EXISTS idx_application_cycles_status RENAME TO idx_proposal_announcements_status;
ALTER INDEX IF EXISTS idx_application_cycles_dates RENAME TO idx_proposal_announcements_dates;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'application_cycles_pkey') THEN
        ALTER TABLE proposal_announcements RENAME CONSTRAINT application_cycles_pkey TO proposal_announcements_pkey;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cycle_dates') THEN
        ALTER TABLE proposal_announcements RENAME CONSTRAINT chk_cycle_dates TO chk_announcement_dates;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cycle_status') THEN
        ALTER TABLE proposal_announcements RENAME CONSTRAINT chk_cycle_status TO chk_announcement_status;
    END IF;
END $$;
