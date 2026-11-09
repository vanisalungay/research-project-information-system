-- Migration: needs_budget flag on proposals
-- ---------------------------------------------------------------------------
-- The Office of the Chancellor records, during final approval, whether the
-- proposal requires the OVCAF funding step. `Proposal.needsBudget` maps this
-- column; it was previously created only by Hibernate (ddl-auto=update) and by
-- the destructive development reset script (schema.sql), so a database
-- provisioned from backend/migration-*.sql alone was missing it and failed
-- Hibernate schema validation with:
--
--   Schema-validation: missing column [needs_budget] in table [proposals]
--
-- Characteristics:
--   * Additive and nullable -> historical proposals keep a NULL value, which
--                              the backend already treats as "not yet decided".
--   * Idempotent            -> ADD COLUMN IF NOT EXISTS; safe to re-run.
--   * Non-destructive       -> no table, row or other column is changed.
-- ---------------------------------------------------------------------------

ALTER TABLE proposals ADD COLUMN IF NOT EXISTS needs_budget BOOLEAN;

COMMENT ON COLUMN proposals.needs_budget IS
    'Set by the Office of the Chancellor at final approval: TRUE = require the OVCAF funding step, FALSE = implement immediately, NULL = not yet decided.';
