-- Rollback for migration-add-revision-deadline-index.sql
DROP INDEX IF EXISTS idx_proposals_revision_deadline;
