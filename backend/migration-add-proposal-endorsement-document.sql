-- Migration: Endorsement document of a proposal (Certification & Endorsement step)
-- ---------------------------------------------------------------------------
-- The "Endorsed By" section used to ask the Proponent for a signature image that was
-- only used to print the certification page - nothing about it reached the proposal
-- record, so the document could not be reopened, downloaded by a reviewer, or audited.
-- It now accepts a PDF only, and this migration adds the two columns that keep the
-- uploaded document with its own proposal:
--
--   endorsement_file_name  the Proponent's own file name, kept for display.
--   endorsement_file_path   the stored PDF, relative to "uploads" (the value the
--                           authorized route GET /api/files?path=... serves).
--
-- Nothing is dropped, renamed or rewritten here:
--
--   * Every existing proposal keeps NULL in both columns, which is exactly the state
--     "no endorsement document was ever uploaded" - so old records keep working and
--     nothing about them changes.
--   * Any endorsement or signature image a proposal already references (the Dean's
--     issued letter, a stored signature) lives in its own columns/tables and is
--     untouched.
--   * The PDF-only rule applies to new uploads and replacements only. Replacing a
--     document removes the file the same proposal stored before; it can never remove a
--     historical file of any other record.
--
-- Safe to run on existing databases: additive, nullable and re-runnable.
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS endorsement_file_name VARCHAR(255);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS endorsement_file_path VARCHAR(500);

-- No index: the document is only ever read together with its proposal.
