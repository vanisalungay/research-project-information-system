-- Migration: Performance indexes for RPIS
-- ---------------------------------------------------------------------------
-- Adds the indexes justified by the repository query patterns. This project has
-- no automatic migration runner (no Flyway/Liquibase); like the other
-- backend/migration-*.sql files it must be applied manually, e.g.:
--
--   psql -h localhost -U postgres -d rpis_db -f migration-add-performance-indexes.sql
--
-- Characteristics:
--   * Idempotent        -> CREATE INDEX IF NOT EXISTS; safe to re-run.
--   * Non-destructive   -> adds indexes only. No table, row, constraint or
--                          sequence is created, altered, deleted or reset.
--   * No duplicates     -> verified against pg_indexes: no existing primary-key,
--                          unique-constraint or equivalent index covers these
--                          column sets.
--
-- For large production tables, replace each statement with its CONCURRENTLY form
-- (CREATE INDEX CONCURRENTLY IF NOT EXISTS ...). CONCURRENTLY cannot run inside a
-- transaction block, so execute each statement on its own (psql autocommit), then
-- confirm no invalid indexes remain:
--   SELECT c.relname FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
--    WHERE NOT i.indisvalid;

-- 1) Notifications: per-recipient feed, newest first.
--    Query: NotificationRepository.findByUserOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at
    ON notifications (user_id, created_at DESC);

-- 2) Proposals: proponent ownership filter (+ optional status filter).
--    Queries: findByProponent, findByProponentAndStatus, findByProponentAndStatusIn
CREATE INDEX IF NOT EXISTS idx_proposals_proponent_status
    ON proposals (proponent_id, status);

-- 3) Proposals: staff status views / eligibility "status IN (...)" filters.
--    Queries: findByStatus, findByStatusIn, findEligibleForQuarterlyReport
CREATE INDEX IF NOT EXISTS idx_proposals_status
    ON proposals (status);

-- 4) Proposals: announcement link (delete-guard + DISTINCT lookup).
--    Query: findAnnouncementIdsWithSubmittedProposals
CREATE INDEX IF NOT EXISTS idx_proposals_announcement_id
    ON proposals (proposal_announcement_id);

-- 5) Quarterly progress reports: per-proposal history, newest first.
--    Queries: findByProposalIdOrderByCreatedAtDesc / ...Asc
CREATE INDEX IF NOT EXISTS idx_qpr_proposal_created_at
    ON quarterly_progress_reports (proposal_id, created_at);

-- 6) Reviews / reports / validations by proposal.
--    Queries: ProposalReviewRepository.findByProposal;
--             ProjectReportRepository.findByProposalId / ...AndReportType / findByReviewStatus;
--             OvcafValidationRepository.findByProposalId / findTopByProposalIdOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_proposal_reviews_proposal
    ON proposal_reviews (proposal_id);
CREATE INDEX IF NOT EXISTS idx_project_reports_proposal
    ON project_reports (proposal_id);
CREATE INDEX IF NOT EXISTS idx_project_reports_review_status
    ON project_reports (review_status);
CREATE INDEX IF NOT EXISTS idx_ovcaf_validations_proposal_created_at
    ON ovcaf_validations (proposal_id, created_at DESC);

-- 7) Proposal child collections (Hibernate loads each by proposal_id; the FK
--    indexes also speed up ON DELETE CASCADE).
CREATE INDEX IF NOT EXISTS idx_proposal_sites_proposal
    ON proposal_sites (proposal_id);
CREATE INDEX IF NOT EXISTS idx_proposal_log_frames_proposal
    ON proposal_log_frames (proposal_id);
CREATE INDEX IF NOT EXISTS idx_proposal_personnel_proposal
    ON proposal_personnel (proposal_id);
CREATE INDEX IF NOT EXISTS idx_proposal_budget_proposal
    ON proposal_budget (proposal_id);
CREATE INDEX IF NOT EXISTS idx_proposal_other_projects_proposal
    ON proposal_other_projects (proposal_id);
CREATE INDEX IF NOT EXISTS idx_proposal_priority_agendas_proposal
    ON proposal_priority_agendas (proposal_id);
CREATE INDEX IF NOT EXISTS idx_proposal_limitations_proposal
    ON proposal_limitations (proposal_id);

-- Verification (run after applying; expect the 16 idx_* names above):
--   SELECT indexname, indexdef FROM pg_indexes
--    WHERE schemaname = 'public' AND indexname LIKE 'idx_%'
--    ORDER BY indexname;
