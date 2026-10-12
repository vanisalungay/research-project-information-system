-- Migration: Reporting-stage documents for implementation-phase reporting
-- ---------------------------------------------------------------------------
-- Extends quarterly_progress_reports so that reporting stages 1-3 can carry a
-- completed Project Workplan (ORPS Inhouse Project Workplan format) alongside
-- the quarterly progress report, and the final (fourth) reporting stage can
-- carry a completed Terminal Report (ORPS Inhouse Terminal Report format).
--
-- Safe to run on existing databases: the change is additive, existing report
-- rows and uploaded files are preserved, and report_type defaults to
-- QUARTERLY_PROGRESS so legacy records keep their current behaviour.
ALTER TABLE quarterly_progress_reports
    ADD COLUMN IF NOT EXISTS report_type VARCHAR(30) NOT NULL DEFAULT 'QUARTERLY_PROGRESS',
    ADD COLUMN IF NOT EXISTS workplan_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS workplan_file_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS terminal_report_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS terminal_report_file_path VARCHAR(500);

-- Ensure legacy rows (created before this migration) are treated as quarterly
-- progress reports.
UPDATE quarterly_progress_reports
    SET report_type = 'QUARTERLY_PROGRESS'
    WHERE report_type IS NULL;
