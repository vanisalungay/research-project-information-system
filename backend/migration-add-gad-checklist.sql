-- Migration: interactive GAD Checklist 2 for Section 19 (GAD Score)
-- ---------------------------------------------------------------------------
-- Section 19 used to be a single uploaded GAD checklist file (`gad_file_name`). It is now the
-- official "GAD CHECKLIST 2: FOR THE PROJECT IDENTIFICATION AND DESIGN STAGES", accomplished
-- in RPIS as 17 scored assessment items, each with a comments field.
--
--   gad_checklist_json  the responses (NO / PARTLY / YES per item), the comments and the
--                       derived total score / interpretation / completion state, as JSON. The
--                       backend recalculates the derived values on every save (see
--                       GadChecklistScoring), so a score sent by a client is never trusted.
--
-- Safe to run on existing databases: the change is additive and nullable. A proposal saved
-- before the checklist - or one that still carries an uploaded GAD document - keeps
-- gad_file_name untouched and simply has a NULL checklist until the proponent fills it in.
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS gad_checklist_json TEXT;

-- No index: the checklist is only ever read together with its proposal.
