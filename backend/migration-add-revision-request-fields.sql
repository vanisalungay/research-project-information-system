-- Migration: Return-for-Revision routing fields
-- ---------------------------------------------------------------------------
-- When an authorized reviewing office returns a proposal for revision, the
-- proposal goes to RPS first. RPS is the only office that can set the revision
-- deadline and forward the revision request to the Proponent. These columns
-- preserve the office's original remarks + attribution and the RPS forward/deadline
-- metadata without overwriting any submission/version history.
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS returned_by_office VARCHAR(50);
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS returned_by_name VARCHAR(255);
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS returned_by_user_id BIGINT;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS returned_at TIMESTAMP;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS return_remarks TEXT;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS revision_deadline TIMESTAMP;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS revision_forwarded_at TIMESTAMP;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS revision_forwarded_by_name VARCHAR(255);
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS revision_forwarded_by_user_id BIGINT;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS revision_notes TEXT;
