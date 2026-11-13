-- Migration: Field-level revision remarks
-- ---------------------------------------------------------------------------
-- Extends the section-remark table so a reviewer can attach a remark to a
-- specific FIELD (or a specific repeated entry) of a proposal instead of only to
-- a whole section. A row with a NULL field_key keeps the original section-level
-- behaviour, so existing rows remain valid.
--
-- Field identity is a stable data path, never a display label, e.g.
--   projectTitle                 scalar field
--   personnel[1]                 a repeated entry (whole row)
--   personnel[1].time            a cell of a repeated entry
--   priorityAgenda.kalikasan.value   a keyed entry
--
-- The backend regenerates child row ids on every submission, so entry_id is only
-- a best-effort hint valid within the same revision cycle. entry_snapshot stores
-- the entry's identifying content and entry_index its position, which together
-- let the proponent UI resolve the remark after rows are added, removed or
-- reordered without ever silently attaching it to a different entry.
--
-- Also persists two fields the forms already collect but never stored:
--   * proposals.project_leader_sex  ("Sex" in Project Profile)
--   * proposals.limitations_text    (Limitations of the Project, section 20)
--
-- Idempotent and safe to run repeatedly. In development Hibernate also creates
-- the columns (spring.jpa.hibernate.ddl-auto=update). Running this script is
-- required when provisioning a database outside the running application.

-- 1. Field-level remark columns -------------------------------------------------
ALTER TABLE proposal_section_remarks ADD COLUMN IF NOT EXISTS field_key VARCHAR(120);
ALTER TABLE proposal_section_remarks ADD COLUMN IF NOT EXISTS field_label VARCHAR(255);
ALTER TABLE proposal_section_remarks ADD COLUMN IF NOT EXISTS entry_index INT;
ALTER TABLE proposal_section_remarks ADD COLUMN IF NOT EXISTS entry_id BIGINT;
ALTER TABLE proposal_section_remarks ADD COLUMN IF NOT EXISTS entry_snapshot TEXT;

COMMENT ON COLUMN proposal_section_remarks.field_key IS
    'Stable field path within the section, e.g. projectTitle, personnel[1].time. NULL = section-level remark.';
COMMENT ON COLUMN proposal_section_remarks.entry_snapshot IS
    'JSON snapshot of the repeated entry as reviewed, used to re-align the remark after edits/reordering.';

-- Fast lookup of a proposal's remarks for one field.
CREATE INDEX IF NOT EXISTS idx_psr_proposal_field
    ON proposal_section_remarks(proposal_id, field_key);

-- 2. Persist the two previously dropped proposal fields -------------------------
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS project_leader_sex VARCHAR(20);
ALTER TABLE proposals ADD COLUMN IF NOT EXISTS limitations_text TEXT;

COMMENT ON COLUMN proposals.project_leader_sex IS 'Sex of the project leader (Project Profile section).';
COMMENT ON COLUMN proposals.limitations_text IS 'Free-text Limitations of the Project (section 20).';
