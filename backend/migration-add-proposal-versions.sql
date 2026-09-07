-- Migration: Submission & Revision History (proposal_versions table)
-- ---------------------------------------------------------------------------
-- Preserves an immutable, append-only history of every proposal submission and
-- revision. The live "proposals" row always reflects the latest version, while
-- this table retains the full snapshot of each prior submission so that no
-- submission can ever be lost, deleted, or overwritten by a newer version.
CREATE TABLE IF NOT EXISTS proposal_versions (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    document_id VARCHAR(100),
    status VARCHAR(50),
    submitted_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    submitted_by_name VARCHAR(255),
    submission_date TIMESTAMP,
    revision_date TIMESTAMP,
    revision_remarks TEXT,
    details_json TEXT,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_proposal_versions UNIQUE (proposal_id, version_number)
);

-- Index for fast lookup of a proposal's version history.
CREATE INDEX IF NOT EXISTS idx_proposal_versions_proposal ON proposal_versions(proposal_id);
