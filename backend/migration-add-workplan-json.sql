-- Migration: Interactive (structured) Project Workplan
-- ---------------------------------------------------------------------------
-- Adds the workplan_json column to quarterly_progress_reports so reporting
-- stages 1-3 can store the MSUN Internally Funded Project Workplan that the
-- proponent fills out in the system (objectives with monthly target activities
-- and accomplishments, per-objective totals, remarks and the "As of" date).
--
-- Safe to run on existing databases: the change is additive, existing report
-- rows are preserved, and the legacy uploaded workplan file columns
-- (workplan_file_name / workplan_file_path) are left untouched so historical
-- workplan attachments keep working.
ALTER TABLE quarterly_progress_reports
    ADD COLUMN IF NOT EXISTS workplan_json TEXT;
