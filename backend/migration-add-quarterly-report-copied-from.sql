-- Migration: trace the report a quarterly progress report was copied from
-- ---------------------------------------------------------------------------
-- The Proponent Side lets a proponent start the next quarterly reporting period from a
-- previous report ("Create a Copy"). The new report is still its own record, created and
-- saved by the existing Create Quarterly Report workflow; this column only records which
-- report was used as its starting point.
--
--   copied_from_report_id   The quarterly_progress_reports.id the new draft was started
--                           from. NULL for every report written from scratch. Deliberately
--                           a plain id without a foreign key or cascade: the source report
--                           is only referenced for traceability and is never modified or
--                           removed by this record.
--
-- Only a report of the same project is ever stored (the service ignores anything else).
--
-- Safe to run on existing databases: the change is additive and nullable, so every
-- existing report simply has no source report.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate also adds this column
-- automatically; this script documents/keeps the DDL explicit.

ALTER TABLE quarterly_progress_reports
    ADD COLUMN IF NOT EXISTS copied_from_report_id BIGINT;
