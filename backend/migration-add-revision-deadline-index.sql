-- Migration: support the hourly revision-deadline reminder query
-- ---------------------------------------------------------------------------
-- The reminder job looks for proposals whose revision_deadline falls inside the next
-- few days. A partial index keeps that lookup bounded as the proposal table grows.
-- Additive and idempotent.
CREATE INDEX IF NOT EXISTS idx_proposals_revision_deadline
    ON proposals (revision_deadline)
    WHERE revision_deadline IS NOT NULL;
