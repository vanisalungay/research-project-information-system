-- Migration: Section-specific revision remarks
-- ---------------------------------------------------------------------------
-- Reviewers can leave a dedicated remark for each individual proposal section
-- instead of putting every revision instruction into one general remarks field.
-- Each row is bound to exactly one proposal and one proposal section (the
-- canonical section key "01".."27" shared with the proposal form and the
-- Proponent's Revision Page).
--
-- Remarks are grouped into revision ROUNDS:
--   * a reviewing office returning a proposal for revision opens a new round and
--     every previously ACTIVE remark is demoted to SUPERSEDED (never overwritten),
--   * RPS forwarding the same revision request appends to the round that is
--     already open, so both offices' feedback stays visible per section.
-- This keeps the feedback of every revision cycle as history while the Proponent
-- only has to work through the ACTIVE round.
--
-- Rows are never deleted or modified by the proposal submission/revision flow, so
-- the remarks survive the Proponent editing and resubmitting the proposal.
--
-- Idempotent and safe to run repeatedly. The table is also created automatically
-- in development by Hibernate (spring.jpa.hibernate.ddl-auto=update). Running this
-- script is required when provisioning a database outside the running application.
CREATE TABLE IF NOT EXISTS proposal_section_remarks (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    section_key VARCHAR(10) NOT NULL,              -- canonical "01".."27"
    section_label VARCHAR(150),                    -- human-readable section title
    remark TEXT NOT NULL,
    round INT NOT NULL,                            -- 1-based revision round
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SUPERSEDED
    reviewer_role VARCHAR(50),                     -- REC | OVCRIGE | RPS
    reviewer_name VARCHAR(255),
    reviewer_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Fast lookup of a proposal's remark history / currently open round.
CREATE INDEX IF NOT EXISTS idx_psr_proposal_round
    ON proposal_section_remarks(proposal_id, round, status);

-- Fast lookup of a proposal's remarks for a single section.
CREATE INDEX IF NOT EXISTS idx_psr_proposal_section
    ON proposal_section_remarks(proposal_id, section_key);
