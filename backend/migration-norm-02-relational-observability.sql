-- =============================================================================
-- RPIS Migration NORM-02: referential-defect observability (READ-ONLY views)
-- =============================================================================
-- PURPOSE
--   NORM-01 closed every foreign key that could be closed safely. The defects
--   that remain CANNOT be expressed as a constraint, because the data itself is
--   invalid and no parent row exists. Those rows must not be deleted, nulled or
--   re-pointed without an owner decision.
--
--   This migration therefore adds nothing but READ-ONLY VIEWS. No table, column,
--   constraint or row is created, altered, dropped or rewritten. The views make
--   the outstanding defects measurable, so progress towards zero can be tracked
--   and a future NOT VALID foreign key can be judged on real numbers.
--
-- DELIBERATELY NOT DONE (each with its evidence)
--   * notifications.proposal_id               255 orphan rows   -> no FK added
--   * notifications.proposal_announcement_id  232 orphan rows   -> no FK added
--   * endorsement_request_events.actor_id       2 orphan rows   -> no FK (preserved)
--   * proposal_priority_agendas.agenda_key      36 unmatched    -> no FK (target not unique)
--   * proposal_versions.document_id -> proposals.document_id   -> REJECTED:
--       13 of 35 version rows cite a HISTORICAL document code (REV00/REV01/...)
--       while proposals.document_id holds only the CURRENT code, and
--       ProposalService regenerates proposals.document_id on every revision. An FK
--       would fail against the existing snapshots AND block the revision workflow.
--       It is a historical snapshot, not a relationship.
--   * proposals.reviewer_id - all 26 values NULL, unmapped legacy column -> no FK
--   * proposal_section_remarks.entry_id, proposals.reviewed_by - no parent key
--   * application_cycles - 0 rows, no entity/repository/service/controller, no FK
--       in either direction. Retain-or-retire decision required; NOTHING changed.
--
-- ROLLBACK: rollback-norm-02-relational-observability.sql drops these 5 views.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

-- 1) Notifications whose proposal_id has no matching proposal (255 rows today)
CREATE OR REPLACE VIEW public.v_orphan_notifications_proposal AS
SELECT n.id, n.user_id, n.proposal_id, n.type, n.created_at
  FROM public.notifications n
 WHERE n.proposal_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM public.proposals p WHERE p.id = n.proposal_id);

-- 2) Notifications whose proposal_announcement_id has no matching announcement (232 rows)
CREATE OR REPLACE VIEW public.v_orphan_notifications_proposal_announcement AS
SELECT n.id, n.user_id, n.proposal_announcement_id, n.type, n.created_at
  FROM public.notifications n
 WHERE n.proposal_announcement_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM public.proposal_announcements a
                    WHERE a.id = n.proposal_announcement_id);

-- 3) Endorsement events whose actor_id has no matching user (2 rows, historical)
CREATE OR REPLACE VIEW public.v_orphan_endorsement_request_events_actor AS
SELECT e.id, e.endorsement_request_id, e.actor_id, e.event_type, e.created_at
  FROM public.endorsement_request_events e
 WHERE e.actor_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM public.users u WHERE u.id = e.actor_id);

-- 4) Proposal agenda selections whose agenda_key matches no configured agenda (36 rows)
CREATE OR REPLACE VIEW public.v_unmatched_proposal_priority_agendas AS
SELECT z.id, z.proposal_id, z.agenda_key, z.selected
  FROM public.proposal_priority_agendas z
 WHERE NOT EXISTS (SELECT 1 FROM public.priority_agendas p
                    WHERE p.agenda_key = z.agenda_key);

-- 5) One-row summary of every outstanding referential defect
CREATE OR REPLACE VIEW public.v_referential_integrity_summary AS
SELECT
  (SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='public' AND c.relkind='r')                              AS tables,
  (SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace
    AND contype='f')                                                         AS foreign_keys,
  (SELECT count(*) FROM public.v_orphan_notifications_proposal)              AS orphan_notifications_proposal,
  (SELECT count(*) FROM public.v_orphan_notifications_proposal_announcement) AS orphan_notifications_announcement,
  (SELECT count(*) FROM public.v_orphan_endorsement_request_events_actor)    AS orphan_endorsement_event_actor,
  (SELECT count(*) FROM public.v_unmatched_proposal_priority_agendas)        AS unmatched_agenda_keys,
  -- A blank string is the column's "no SDGs selected" value (the legacy rows
  -- store '' rather than NULL), so it is not a parse failure and must not be
  -- reported as one.
  (SELECT count(*) FROM public.proposals
    WHERE sdg IS NOT NULL AND btrim(sdg) <> '' AND NOT (sdg IS JSON))
                                                                             AS nonparsable_proposals_sdg,
  ((SELECT count(*) FROM public.v_orphan_notifications_proposal)
   + (SELECT count(*) FROM public.v_orphan_notifications_proposal_announcement)
   + (SELECT count(*) FROM public.v_orphan_endorsement_request_events_actor)
   + (SELECT count(*) FROM public.v_unmatched_proposal_priority_agendas))    AS total_unresolved_defects;

COMMIT;

-- VERIFICATION (read after COMMIT)
SELECT 'NORM-02 views present (expected 5)' AS check_name, count(*)::text AS value
  FROM pg_views WHERE schemaname='public' AND viewname IN (
    'v_orphan_notifications_proposal','v_orphan_notifications_proposal_announcement',
    'v_orphan_endorsement_request_events_actor','v_unmatched_proposal_priority_agendas',
    'v_referential_integrity_summary');
SELECT 'NORM-02 tables unchanged (expected 29)' AS check_name,
       (SELECT count(*)::text FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
         WHERE n.nspname='public' AND c.relkind='r');
SELECT * FROM public.v_referential_integrity_summary;
