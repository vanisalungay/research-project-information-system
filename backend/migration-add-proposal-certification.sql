-- Migration: Certification & Endorsement metadata of a proposal
-- ---------------------------------------------------------------------------
-- The Certification & Endorsement step ("Submitted By" / "Endorsed By") used to keep
-- its name, designation and date fields only inside the open form: saving a draft and
-- reopening it started from empty fields, and a reviewer opening the submitted proposal
-- could not see the certification at all. The proponent's own signature was likewise
-- local to the step, so a saved draft forgot it.
--
-- This migration adds the columns that keep that data with its own proposal:
--
--   submitted_by_name                    the Proponent's typed name.
--   submitted_by_designation             the Proponent's designation.
--   submitted_by_date                    the typed date (kept verbatim, like the form).
--   submitted_by_file_name               the Proponent's own signature file name.
--   submitted_by_file_path               the stored signature, relative to "uploads"
--                                        (the value GET /api/files?path=... serves).
--   endorsed_by_name                     the endorser's typed name.
--   endorsed_by_designation              the endorser's designation.
--   endorsed_by_date                     the typed date.
--
-- Nothing is dropped, renamed or rewritten here:
--
--   * Every existing proposal keeps NULL in all eight columns, which is exactly the
--     state "this proposal was saved before the certification step persisted its data".
--     Old records keep working and nothing about them changes.
--   * The endorsement document ("Endorsed By" PDF) already has its own columns
--     (endorsement_file_name / endorsement_file_path) from a previous migration; they
--     are not touched.
--   * The display name and stored path are written only by the signature upload
--     endpoint, never accepted from the JSON proposal DTO, so a client cannot point the
--     proposal at an arbitrary stored file.
--
-- Safe to run on existing databases: additive, nullable and re-runnable.
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS submitted_by_name VARCHAR(255);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS submitted_by_designation VARCHAR(255);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS submitted_by_date VARCHAR(50);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS submitted_by_file_name VARCHAR(255);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS submitted_by_file_path VARCHAR(500);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS endorsed_by_name VARCHAR(255);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS endorsed_by_designation VARCHAR(255);

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS endorsed_by_date VARCHAR(50);

-- No index: the certification is only ever read together with its proposal.
