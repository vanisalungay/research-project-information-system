-- Migration: Technology Map image upload on a proposal
-- ---------------------------------------------------------------------------
-- Section 13 of the Create Proposal form (the Technology Roadmap / Technology
-- Map section) used to keep the chosen file in the browser only: the proponent
-- picked a document, the file name travelled with the proposal, but the bytes
-- were never stored, so no reviewer could ever open it.
--
-- The proponent now uploads the technology map as an image (PNG or JPEG, at
-- most 10 MB) and the file is stored under `uploads/technology-maps/`. Two
-- columns describe it:
--
--   roadmap_file_name  the file name the proponent selected - kept for display,
--                      as before, so every existing screen keeps showing it.
--   roadmap_file_path  the stored file, relative to the `uploads` directory
--                      (e.g. `technology-maps/<uuid>_technology-map.png`). The
--                      authorized download route (GET /api/files?path=...) and
--                      the per-proposal ownership check use this value.
--
-- Existing rows are untouched: `roadmap_file_name` keeps its current value and
-- `roadmap_file_path` stays NULL until the proponent uploads an image again, so
-- nothing that was saved before is lost.
--
-- Safe to run on existing databases: the change is additive and nullable.
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS roadmap_file_path VARCHAR(500);

-- No index: the path is only ever read together with its proposal, never
-- searched by value.
