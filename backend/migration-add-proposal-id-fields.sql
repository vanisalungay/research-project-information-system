-- Migration: Add Proposal ID and Revision tracking fields
-- This migration adds the new columns for the formatted document ID system
-- Format: MSUN-ORPS-DA-{proposalCode}-{year}-REV{revisionNo}
-- Example: MSUN-ORPS-DA-001-2026-REV00

-- Add new columns to proposals table
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS proposal_code VARCHAR(10) UNIQUE;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS revision_number INT DEFAULT 0;
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS document_id VARCHAR(100) UNIQUE;

-- Add comment for documentation
COMMENT ON COLUMN proposals.proposal_code IS '3-digit continuous counter: 001, 002, etc.';
COMMENT ON COLUMN proposals.revision_number IS '0 = initial submission, 1 = first revision, etc.';
COMMENT ON COLUMN proposals.document_id IS 'Full format: MSUN-ORPS-DA-001-2026-REV00';

-- Note: Existing proposals will have NULL values for these fields.
-- New proposals will automatically get generated values from the ProposalService.
-- To backfill existing proposals, you can run a script to generate codes based on their ID order.