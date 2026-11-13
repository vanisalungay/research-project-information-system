-- Migration: Unique revision identifier on proposal_versions
-- ---------------------------------------------------------------------------
-- Guarantees, at the database level, that a single proposal can never have two
-- version rows with the same version number. This is the revision-identifier
-- safety net for concurrent revision resubmissions; the application already
-- serializes per-proposal resubmissions with a row-level lock
-- (ProposalRepository.findByIdForUpdate).
--
-- This script is:
--   * data-preserving  -> it never deletes, renumbers or modifies existing rows.
--   * guarded          -> it aborts with a clear error if duplicate
--                         (proposal_id, version_number) rows already exist.
--   * idempotent       -> safe to run repeatedly (skips when the constraint is
--                         already present).
--
-- Apply with:  psql -h localhost -U postgres -d rpis_db -f migration-add-proposal-version-unique.sql
-- (or any SQL client). Hibernate's ddl-auto also creates the constraint for
-- fresh databases because ProposalVersion now declares it via @UniqueConstraint.

DO $$
BEGIN
    -- Guard: refuse to proceed if existing data already violates the rule.
    IF EXISTS (
        SELECT 1
          FROM proposal_versions
         GROUP BY proposal_id, version_number
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot add uq_proposal_versions: duplicate (proposal_id, version_number) rows exist';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_proposal_versions'
    ) THEN
        ALTER TABLE proposal_versions
            ADD CONSTRAINT uq_proposal_versions UNIQUE (proposal_id, version_number);
    END IF;
END $$;

-- Supporting index for fast lookup of a proposal's version history.
CREATE INDEX IF NOT EXISTS idx_proposal_versions_proposal ON proposal_versions(proposal_id);
