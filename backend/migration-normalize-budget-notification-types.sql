-- ---------------------------------------------------------------------------
-- RPIS migration: budget-return notifications use the FUNDING type
--
-- Notifications written before the Proponent notification revision stored budget
-- returns as REVISION. The notification destination now sends a Proponent's REVISION
-- row to the revision form, while a budget return belongs on the proposal details
-- (where the financial remarks are shown). Re-typing the historical budget rows keeps
-- that routing correct.
--
-- Only the two budget-specific titles are changed; proposal-revision rows keep REVISION
-- and continue to open the revision form.
-- Idempotent: running it again matches nothing.
-- ---------------------------------------------------------------------------

BEGIN;

UPDATE public.notifications
SET type = 'FUNDING'
WHERE type = 'REVISION'
  AND title IN ('Budget Revision Required', 'Budget Request Returned');

COMMIT;
