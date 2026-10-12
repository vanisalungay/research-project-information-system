-- Migration: link proposals to the proposal announcement (submission window)
-- they were created/submitted under.
--
-- This lets RPS enforce the rule that a Proposal Announcement with submitted
-- proposals cannot be deleted. The column is nullable so historical proposals
-- (created before this feature existed) are unaffected.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate will also add this
-- column automatically; this script documents/keeps the DDL explicit.

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS proposal_announcement_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_proposals_announcement_id ON proposals (proposal_announcement_id);
