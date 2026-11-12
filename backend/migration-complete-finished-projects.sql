-- ===========================================================================
-- Migration: complete projects whose required reporting is already finished
-- ===========================================================================
-- The project completion transition was missing from the application: submitting
-- Q1, Q2, Q3 and the Terminal Report left the proposal at APPROVED
-- ("Implementation Approved") indefinitely. New final submissions now persist
-- COMPLETED in the same transaction (QuarterlyProgressReportService), and this
-- script corrects the projects that finished reporting before that fix.
--
-- WHAT THIS FILE DOES
--   Moves a project from an implementation-phase status (APPROVED, RELEASED or
--   IMPLEMENTATION) to COMPLETED, and only when the project's own records already
--   prove that its required reporting is finished:
--     * a SUBMITTED Q1, Q2 and Q3 QUARTERLY_PROGRESS report, and
--     * a SUBMITTED TERMINAL_REPORT, and
--     * the approval-chain evidence the reporting service itself requires:
--       an issued SO number recorded by the OC (so_uploaded_at set), plus the
--       OVCAF APPROVED_ENDORSED decision when needs_budget is TRUE.
--   proposals.status and proposals.updated_at are updated, and one
--   proposal_status_history row (source 'DATA_CORRECTION') records the transition
--   from the status the project actually held.
--
-- WHAT IS DELIBERATELY NOT DONE
--   * No report, report document, attachment or other proposal field is updated,
--     and nothing is deleted.
--   * A project with any missing, DRAFT, or returned report is never completed.
--   * No report approval is invented and no actor is invented: the history row
--     carries no actor_user_id/actor_role, only the evidence-backed reason.
--
-- IDEMPOTENCE
--   The status guard excludes COMPLETED, so a second run updates 0 rows and
--   inserts 0 history rows.
--
-- NORMALIZATION (must match QuarterlyProgressReportService exactly)
--   * Proposal status: the service compares trim().toUpperCase(), so a stored
--     status with surrounding spaces or different case still counts.
--   * Report period: the service compares with equalsIgnoreCase() and does NOT
--     trim; a period with surrounding whitespace does not match. No btrim() is
--     used below, deliberately.
--   * Report type: the service's typeOf() treats NULL and blank (empty or
--     whitespace-only) as QUARTERLY_PROGRESS and otherwise compares the value
--     exactly; the CASE below mirrors that.
--   * Report status: the service requires exactly 'SUBMITTED'; the raw
--     comparison below is exact for the same reason.
--
-- LEGACY ROWS
--   quarterly_progress_reports.report_type is NULL on rows created before the
--   column existed; typeOf() treats those as QUARTERLY_PROGRESS, which the CASE
--   below mirrors.
-- ===========================================================================

WITH finished AS (
    SELECT p.id, upper(btrim(p.status)) AS from_status
    FROM proposals p
    WHERE upper(btrim(p.status)) IN ('APPROVED', 'RELEASED', 'IMPLEMENTATION')
      AND p.so_number IS NOT NULL
      AND btrim(p.so_number) <> ''
      AND p.so_uploaded_at IS NOT NULL
      AND (
            p.needs_budget IS DISTINCT FROM TRUE
            OR EXISTS (
                SELECT 1
                FROM ovcaf_validations v
                WHERE v.proposal_id = p.id
                  AND v.decision = 'APPROVED_ENDORSED'
                  AND v.budget_endorsed = TRUE
            )
          )
      AND EXISTS (
            SELECT 1 FROM quarterly_progress_reports r
            WHERE r.proposal_id = p.id
              AND (CASE
                       WHEN r.report_type IS NULL
                            OR r.report_type ~ '^[[:space:]]*$'
                       THEN 'QUARTERLY_PROGRESS'
                       ELSE r.report_type
                   END) = 'QUARTERLY_PROGRESS'
              AND upper(r.period) = 'Q1'
              AND r.status = 'SUBMITTED'
          )
      AND EXISTS (
            SELECT 1 FROM quarterly_progress_reports r
            WHERE r.proposal_id = p.id
              AND (CASE
                       WHEN r.report_type IS NULL
                            OR r.report_type ~ '^[[:space:]]*$'
                       THEN 'QUARTERLY_PROGRESS'
                       ELSE r.report_type
                   END) = 'QUARTERLY_PROGRESS'
              AND upper(r.period) = 'Q2'
              AND r.status = 'SUBMITTED'
          )
      AND EXISTS (
            SELECT 1 FROM quarterly_progress_reports r
            WHERE r.proposal_id = p.id
              AND (CASE
                       WHEN r.report_type IS NULL
                            OR r.report_type ~ '^[[:space:]]*$'
                       THEN 'QUARTERLY_PROGRESS'
                       ELSE r.report_type
                   END) = 'QUARTERLY_PROGRESS'
              AND upper(r.period) = 'Q3'
              AND r.status = 'SUBMITTED'
          )
      AND EXISTS (
            SELECT 1 FROM quarterly_progress_reports r
            WHERE r.proposal_id = p.id
              AND r.report_type = 'TERMINAL_REPORT'
              AND upper(r.period) = 'TERMINAL'
              AND r.status = 'SUBMITTED'
          )
),
updated AS (
    UPDATE proposals p
    SET status = 'COMPLETED',
        updated_at = CURRENT_TIMESTAMP
    FROM finished f
    WHERE p.id = f.id
    RETURNING p.id
)
-- The transition is recorded in the audit table it belongs to. No report approval or
-- other workflow event is invented.
INSERT INTO proposal_status_history
    (proposal_id, from_status, to_status, remarks, source, changed_at)
SELECT f.id,
       f.from_status,
       'COMPLETED',
       'Data correction: all required reporting stages (Q1, Q2, Q3 and the Terminal '
           || 'Report) were already submitted.',
       'DATA_CORRECTION',
       CURRENT_TIMESTAMP
FROM finished f
JOIN updated u ON u.id = f.id;
