-- Migration: Cooperating Agency directory and the per-proposal selection
-- ---------------------------------------------------------------------------
-- Section 02 of the Create Proposal form used to be one free-text field
-- ("Name/s and Address/es *"). It is now a checklist of cooperating agencies:
--
--   cooperating_agencies            the shared directory: one row per agency,
--                                   reusable by every proposal and offered to
--                                   every proponent.
--   proposal_cooperating_agencies   the join table: one row per agency selected
--                                   on one proposal.
--
-- The proposal keeps its `cooperating_agencies` TEXT column: it is still written
-- on every save, derived from the selected agency names, so the proposal details
-- screen, reviewer views, exports and printouts - which all read that single
-- column - keep showing the agencies.
--
-- Existing rows are not touched: a proposal saved before this change keeps its
-- text and simply has no directory selection yet.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate also creates both
-- tables automatically; this script documents/keeps the DDL explicit.

CREATE TABLE IF NOT EXISTS cooperating_agencies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Case-insensitive uniqueness is the database-level guarantee that the same
-- agency cannot be stored twice, not even when two proponents add it at the same
-- moment or two backend instances seed the directory together. CooperatingAgency-
-- Initializer creates the same index idempotently on startup.
CREATE UNIQUE INDEX IF NOT EXISTS uk_cooperating_agencies_name_lower
    ON cooperating_agencies (LOWER(name));

CREATE TABLE IF NOT EXISTS proposal_cooperating_agencies (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    cooperating_agency_id BIGINT NOT NULL REFERENCES cooperating_agencies(id),
    -- A proposal can select an agency only once.
    CONSTRAINT uk_proposal_cooperating_agency UNIQUE (proposal_id, cooperating_agency_id)
);

CREATE INDEX IF NOT EXISTS idx_proposal_cooperating_agencies_proposal
    ON proposal_cooperating_agencies (proposal_id);

-- Default agencies of the checklist (order of the checklist is the lower-cased
-- name). ON CONFLICT DO NOTHING keeps a restart from inserting them twice and
-- never overwrites a row a user has since edited.
INSERT INTO cooperating_agencies (name) VALUES
    ('Commission on Higher Education (CHED)'),
    ('Department of Agriculture (DA)'),
    ('Department of Education (DepEd)'),
    ('Department of Energy (DOE)'),
    ('Department of Environment and Natural Resources (DENR)'),
    ('Department of Health (DOH)'),
    ('Department of Information and Communications Technology (DICT)'),
    ('Department of Labor and Employment (DOLE)'),
    ('Department of Public Works and Highways (DPWH)'),
    ('Department of Science and Technology (DOST)'),
    ('Department of Social Welfare and Development (DSWD)'),
    ('Department of the Interior and Local Government (DILG)'),
    ('Department of Tourism (DOT)'),
    ('Department of Trade and Industry (DTI)'),
    ('Intellectual Property Office of the Philippines (IPOPHL)'),
    ('Local Government Unit (LGU)'),
    ('National Commission on Indigenous Peoples (NCIP)'),
    ('National Disaster Risk Reduction and Management Council (NDRRMC)'),
    ('National Economic and Development Authority (NEDA)'),
    ('Philippine Space Agency (PhilSA)'),
    ('Philippine Statistics Authority (PSA)')
ON CONFLICT DO NOTHING;
