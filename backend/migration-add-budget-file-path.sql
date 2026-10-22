-- Migration: Line-Item Budget file upload on a proposal
-- ---------------------------------------------------------------------------
-- Section 25 of the Create Proposal form ("Line-Item Budget Requirement") used to
-- keep the chosen document in the browser only: the proponent picked a file, the
-- file name travelled with the proposal, but the bytes were never stored, so no
-- reviewer could ever open the budget that was submitted.
--
-- The proponent now uploads the filled-out line-item budget - a PDF or a DOCX of
-- at most 10 MB - and the file is stored under `uploads/budget-files/`. Two
-- columns describe it:
--
--   beneficiaries_file_name  the file name the proponent selected - kept for
--                            display, as before, so every existing screen keeps
--                            showing it (the field predates this feature).
--   beneficiaries_file_path  the stored file, relative to the `uploads` directory
--                            (e.g. `budget-files/<uuid>_line-item-budget.pdf`). The
--                            authorized download route (GET /api/files?path=...)
--                            and the per-proposal ownership check use this value.
--
-- Existing rows are untouched: `beneficiaries_file_name` keeps its current value
-- and `beneficiaries_file_path` stays NULL until the proponent uploads a file
-- again, so nothing that was saved before is lost.
--
-- Safe to run on existing databases: the change is additive and nullable.
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS beneficiaries_file_path VARCHAR(500);

-- No index: the path is only ever read together with its proposal, never
-- searched by value.
