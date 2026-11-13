-- Migration: Concurrency-safe proposal code allocation
-- ---------------------------------------------------------------------------
-- Replaces the previous application-side "MAX(proposal_code) + 1" allocation,
-- which could hand the same code to two simultaneous submissions.
--
-- proposal_code_seq is a PostgreSQL sequence: nextval() is atomic, so every
-- successful submission (from any number of concurrent requests or backend
-- instances) receives a distinct, increasing code. Existing records are never
-- renumbered, reassigned or deleted.
--
-- The same sequence is also ensured at application startup by
-- ProposalCodeSequenceInitializer, so running this script manually is only
-- required when provisioning a database outside the running application. It is
-- idempotent and safe to run repeatedly.

CREATE SEQUENCE IF NOT EXISTS proposal_code_seq;

-- Align the sequence with the highest existing numeric code so newly generated
-- codes cannot collide with identifiers already present in the database.
--   * existing codes  -> setval(max, is_called = true)  -> nextval returns max + 1
--   * no existing codes -> setval(1, is_called = false) -> nextval returns 1 (001)
SELECT setval(
    'proposal_code_seq',
    GREATEST(
        (SELECT COALESCE(MAX(CAST(proposal_code AS INTEGER)), 1)
           FROM proposals
          WHERE proposal_code ~ '^[0-9]+$'),
        1
    ),
    (SELECT COUNT(*) > 0 FROM proposals WHERE proposal_code ~ '^[0-9]+$')
);

-- Existing database-level guards for the proposal / revision identifiers:
--   * proposals.proposal_code          UNIQUE  (continuous proposal code)
--   * proposals.document_id            UNIQUE  (MSUN-ORPS-DA-<code>-<year>-REV<nn>)
--   * proposal_versions(proposal_id, version_number) UNIQUE (revision identifier)
-- These are the final safety net; the sequence makes proposal-code collisions
-- structurally impossible, and the revision flow serializes per-proposal
-- resubmissions with a row-level lock (ProposalRepository.findByIdForUpdate).
