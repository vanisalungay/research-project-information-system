-- Migration: record the actual RPS endorsement timestamp
--
-- `submitted_at` is when the proponent submitted; `updated_at` keeps moving through every
-- later office. Neither is the RPS endorsement event, which is what the Endorsed
-- Proposals list orders by.
--
-- New endorsements are written by ProposalService when the proposal enters ENDORSED.
-- Existing rows are backfilled from the best recorded event available:
--   1. an ENDORSED transition in the status-history audit, then
--   2. the RPS review record (its creation is the review that accompanied endorsement),
--   3. last-known modification for legacy rows that predate both records.
-- The third fallback is only for history that cannot be reconstructed exactly; from this
-- migration onward the column is written at the event itself.

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS endorsed_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE proposals p
SET endorsed_at = COALESCE(
    (SELECT MAX(h.changed_at)
       FROM proposal_status_history h
      WHERE h.proposal_id = p.id AND h.to_status = 'ENDORSED'),
    (SELECT MAX(r.created_at)
       FROM proposal_reviews r
      WHERE r.proposal_id = p.id),
    p.updated_at
)
WHERE p.status IN (
        'ENDORSED', 'OVCRIGE_COORDINATION', 'UNDER_REVIEW', 'REC_EVALUATION',
        'REC_APPROVED', 'OVC_APPROVED', 'CHANCELLOR_REVIEW', 'FOR_OC_APPROVAL',
        'FOR_OVCAF_APPROVAL', 'FINANCE_AUTHORIZATION', 'READY_FOR_RELEASE',
        'RELEASED', 'IMPLEMENTATION', 'APPROVED', 'APPROVED_ENDORSED',
        'COMPLETED', 'RETURNED'
      )
  AND p.endorsed_at IS NULL;

COMMENT ON COLUMN proposals.endorsed_at IS
    'When RPS endorsed the proposal to OVCRIGE; NULL if it was never endorsed.';

CREATE INDEX IF NOT EXISTS idx_proposals_endorsed_at
    ON proposals (endorsed_at DESC NULLS LAST, id DESC);
