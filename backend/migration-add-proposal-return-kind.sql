-- ===========================================================================
-- Proposal return kind: revision vs rejection
-- ===========================================================================
-- A reviewing office can return a proposal to ORPS (RPS) for two different reasons:
--   * a return-for-revision, which ORPS forwards to the Proponent; and
--   * an OVCRIGE rejection, which ORPS reviews and completes.
-- Both use the existing RETURNED_TO_RPS status, so the reason must be recorded
-- separately. This column is that discriminator; it introduces no new status.
--
-- Idempotent: safe to run on every start.
-- ===========================================================================

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS return_kind VARCHAR(20);

-- Historical returns were revision returns: before this column existed, OVCRIGE's only
-- "return to ORPS" action was the return-for-revision endpoint.
UPDATE proposals
   SET return_kind = 'REVISION'
 WHERE return_kind IS NULL
   AND returned_by_office IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_proposals_return_kind'
    ) THEN
        ALTER TABLE proposals
            ADD CONSTRAINT chk_proposals_return_kind
            CHECK (return_kind IS NULL OR return_kind IN ('REVISION', 'REJECTION'));
    END IF;
END $$;
