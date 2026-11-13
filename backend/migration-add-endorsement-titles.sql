-- Migration: separate Program Title and Project Title on an endorsement request
-- ---------------------------------------------------------------------------
-- A Request for Endorsement carries two distinct titles, exactly like the Create
-- Proposal form:
--
--   * the PROGRAM TITLE - the programme the project belongs to. It is optional: the
--     Proponent may leave it empty, reuse one of their own previous program titles,
--     or create a new one (see components/ProgramTitleSelect.vue);
--   * the PROJECT TITLE - the title of the specific project being endorsed. It is
--     required, it is what the official endorsement letter prints, and it is what the
--     Certifications entry is named after.
--
-- The project title needs NO new column: it is what the existing proposal_title column
-- has always held. The letter body ("...the Internally Funded Project Proposal (IFP)
-- entitled "<title>"...") and the certification title already read it that way, and the
-- Create Proposal carry-over already put it in the proposal's project_title. Renaming
-- the column would rewrite every existing record, so it deliberately keeps its historic
-- name; the application field is named projectTitle and the API exposes it as
-- `projectTitle`.
--
-- The program title had nowhere to live, so it is added here. It is nullable because
-- the field is optional, and its width matches proposals.program_title exactly, so a
-- program title created here is carried over to the proposal without ever being
-- rejected for its length.
--
-- Every change is additive: no existing table, column or record is dropped, renamed or
-- rewritten, and no row is modified. Existing endorsement records therefore keep
-- working unchanged: their proposal_title is already their project title and their
-- program title is simply unknown (NULL), which is exactly how the Create Proposal form
-- treats an empty Program Title.
--
-- Idempotent: safe to run repeatedly.

ALTER TABLE endorsement_requests
    ADD COLUMN IF NOT EXISTS program_title VARCHAR(255);

COMMENT ON COLUMN endorsement_requests.program_title IS
    'Optional Program Title the Proponent selected or created (the programme the project belongs to). NULL when the field was left empty.';

COMMENT ON COLUMN endorsement_requests.proposal_title IS
    'Historic column name. Holds the PROJECT TITLE (API field projectTitle). Kept because renaming it would rewrite existing records.';
