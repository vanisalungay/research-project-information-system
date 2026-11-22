-- Migration: record the actual proposal submission timestamp
--
-- `created_at` is when the draft row was first saved and `updated_at` moves on every
-- edit; neither is the submission event. This column is written when a proposal enters
-- FOR REVIEW (initial submission or a revision resubmission), and is what the Submitted
-- Proposals list orders by.
--
-- Existing rows are backfilled from the immutable version history
-- (proposal_versions.submission_date is written on each submission), so no proposal
-- falls back to its creation date. Rows that never had a submission keep NULL and are
-- sorted last by the application.

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE proposals p
SET submitted_at = versions.last_submission
FROM (
    SELECT proposal_id, MAX(submission_date) AS last_submission
    FROM proposal_versions
    WHERE submission_date IS NOT NULL
    GROUP BY proposal_id
) AS versions
WHERE p.id = versions.proposal_id
  AND p.submitted_at IS NULL;

COMMENT ON COLUMN proposals.submitted_at IS
    'When the proposal was actually submitted/resubmitted into FOR REVIEW; NULL if never submitted.';

-- Supports ORDER BY submitted_at DESC NULLS LAST, id DESC without a sort of the whole table.
CREATE INDEX IF NOT EXISTS idx_proposals_submitted_at
    ON proposals (submitted_at DESC NULLS LAST, id DESC);
