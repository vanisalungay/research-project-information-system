-- =============================================================================
-- RPIS Migration NORM-01: relational integrity (foreign keys) - rpis_db
-- =============================================================================
-- AUTHORISED SCOPE. Implements ONLY the foreign keys whose preconditions were
-- verified against the live catalog of rpis_db (0 orphan rows each).
--
-- APPLIED (15 FKs):  activity_logs.user_id->users RESTRICT |
--   quarterly_progress_reports.proposal_id->proposals RESTRICT |
--   quarterly_progress_reports.created_by->users SET NULL |
--   quarterly_progress_reports.submitted_by->users SET NULL |
--   project_reports.proposal_id->proposals RESTRICT |
--   project_reports.submitted_by->users SET NULL |
--   project_reports.reviewed_by->users SET NULL |
--   proposal_section_remarks.reviewer_id->users SET NULL |
--   proposals.proposal_announcement_id->proposal_announcements RESTRICT |
--   proposal_announcement_updates.actor_id->users SET NULL |
--   proposals.returned_by_user_id->users SET NULL |
--   proposals.revision_forwarded_by_user_id->users SET NULL |
--   ovcaf_validations.released_by->users SET NULL |
--   dean_assignments.created_by->users SET NULL |
--   dean_assignments.revoked_by->users SET NULL
-- All use ON UPDATE NO ACTION.
--
-- DELIBERATELY NOT APPLIED (with reasons):
--   quarterly_progress_reports.copied_from_report_id -> self : EXCLUDED.
--        migration-add-quarterly-report-copied-from.sql documents this as a
--        deliberate plain id with NO foreign key; migration-01 contradicts it.
--        The conflict is unresolved, so nothing is applied. A plain self-FK
--        could not enforce "same proposal" anyway.
--   notifications.proposal_id (255 orphans), notifications.proposal_
--        announcement_id (232 orphans) : data invalid.
--   endorsement_request_events.actor_id (2 orphans; value 79 is a historical
--        artefact migration-01 requires be preserved).
--   proposals.reviewer_id : all 26 values NULL; no demonstrated semantics.
--   proposal_personnel.* : no user/person identity column; no fabricated FK.
--   application_cycles : isolated, empty; no relationship is justified.
--   proposal_priority_agendas.agenda_key -> priority_agendas.agenda_key :
--        36 unmatched values (incl. undefined); target is not a NOT NULL key.
--
-- SAFETY: additive only. No table/column/row created, dropped or rewritten.
-- Constraint names match migration-01, so that file becomes a no-op for these
-- 15 constraints if executed afterwards.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

-- SECTION 1 - PRECONDITIONS (abort before any DDL if any orphan exists)
DO $norm01_guard$
DECLARE r record; n bigint; bad text := '';
BEGIN
    FOR r IN SELECT * FROM (VALUES
        ('activity_logs','user_id','users'),
        ('quarterly_progress_reports','proposal_id','proposals'),
        ('quarterly_progress_reports','created_by','users'),
        ('quarterly_progress_reports','submitted_by','users'),
        ('project_reports','proposal_id','proposals'),
        ('project_reports','submitted_by','users'),
        ('project_reports','reviewed_by','users'),
        ('proposal_section_remarks','reviewer_id','users'),
        ('proposals','proposal_announcement_id','proposal_announcements'),
        ('proposal_announcement_updates','actor_id','users'),
        ('proposals','returned_by_user_id','users'),
        ('proposals','revision_forwarded_by_user_id','users'),
        ('ovcaf_validations','released_by','users'),
        ('dean_assignments','created_by','users'),
        ('dean_assignments','revoked_by','users')
    ) AS t(child_table, child_col, parent_table)
    LOOP
        EXECUTE format('SELECT count(*) FROM public.%I c WHERE c.%I IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.%I p WHERE p.id = c.%I)',
                       r.child_table, r.child_col, r.parent_table, r.child_col) INTO n;
        IF n > 0 THEN
            bad := bad || format(E'\n  - %s.%s : %s orphan row(s) vs %s', r.child_table, r.child_col, n, r.parent_table);
        END IF;
    END LOOP;
    IF bad <> '' THEN
        RAISE EXCEPTION 'NORM-01 PRECONDITION FAILED - unresolved orphan references:%', bad;
    END IF;
    RAISE NOTICE 'NORM-01 preconditions OK: 0 orphans across all 15 candidate foreign keys.';
END $norm01_guard$;

-- SECTION 2 - FOREIGN KEYS (15, idempotent by constraint name)
DO $norm01_fks$
DECLARE e record;
BEGIN
    FOR e IN SELECT * FROM (VALUES
        ('fk_activity_logs_user','activity_logs','user_id','users','RESTRICT'),
        ('fk_qpr_proposal','quarterly_progress_reports','proposal_id','proposals','RESTRICT'),
        ('fk_qpr_created_by','quarterly_progress_reports','created_by','users','SET NULL'),
        ('fk_qpr_submitted_by','quarterly_progress_reports','submitted_by','users','SET NULL'),
        ('fk_project_reports_proposal','project_reports','proposal_id','proposals','RESTRICT'),
        ('fk_project_reports_submitted_by','project_reports','submitted_by','users','SET NULL'),
        ('fk_project_reports_reviewed_by','project_reports','reviewed_by','users','SET NULL'),
        ('fk_psr_reviewer','proposal_section_remarks','reviewer_id','users','SET NULL'),
        ('fk_proposals_proposal_announcement','proposals','proposal_announcement_id','proposal_announcements','RESTRICT'),
        ('fk_pau_actor','proposal_announcement_updates','actor_id','users','SET NULL'),
        ('fk_proposals_returned_by_user','proposals','returned_by_user_id','users','SET NULL'),
        ('fk_proposals_revision_forwarded_by_user','proposals','revision_forwarded_by_user_id','users','SET NULL'),
        ('fk_ovcaf_released_by','ovcaf_validations','released_by','users','SET NULL'),
        ('fk_dean_assignments_created_by','dean_assignments','created_by','users','SET NULL'),
        ('fk_dean_assignments_revoked_by','dean_assignments','revoked_by','users','SET NULL')
    ) AS t(conname, child_table, child_col, parent_table, on_delete)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint c WHERE c.conname = e.conname AND c.connamespace = 'public'::regnamespace) THEN
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.%I(id) ON DELETE %s ON UPDATE NO ACTION',
                           e.child_table, e.conname, e.child_col, e.parent_table, e.on_delete);
        END IF;
    END LOOP;
END $norm01_fks$;

-- SECTION 3 - INDEXES for FK columns lacking coverage (11)
CREATE INDEX IF NOT EXISTS idx_qpr_created_by                   ON public.quarterly_progress_reports (created_by);
CREATE INDEX IF NOT EXISTS idx_qpr_submitted_by                 ON public.quarterly_progress_reports (submitted_by);
CREATE INDEX IF NOT EXISTS idx_project_reports_submitted_by     ON public.project_reports (submitted_by);
CREATE INDEX IF NOT EXISTS idx_project_reports_reviewed_by      ON public.project_reports (reviewed_by);
CREATE INDEX IF NOT EXISTS idx_psr_reviewer                     ON public.proposal_section_remarks (reviewer_id);
CREATE INDEX IF NOT EXISTS idx_pau_actor                        ON public.proposal_announcement_updates (actor_id);
CREATE INDEX IF NOT EXISTS idx_proposals_returned_by            ON public.proposals (returned_by_user_id);
CREATE INDEX IF NOT EXISTS idx_proposals_revision_forwarded_by  ON public.proposals (revision_forwarded_by_user_id);
CREATE INDEX IF NOT EXISTS idx_ovcaf_released_by                ON public.ovcaf_validations (released_by);
CREATE INDEX IF NOT EXISTS idx_dean_assignments_created_by      ON public.dean_assignments (created_by);
CREATE INDEX IF NOT EXISTS idx_dean_assignments_revoked_by      ON public.dean_assignments (revoked_by);

-- SECTION 4 - UNIQUE candidate key on the agenda junction (0 duplicates verified)
DO $norm01_uq$
BEGIN
    IF EXISTS (SELECT 1 FROM public.proposal_priority_agendas GROUP BY proposal_id, agenda_key HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'NORM-01: duplicate (proposal_id, agenda_key) rows exist; aborting';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_proposal_priority_agendas' AND connamespace = 'public'::regnamespace) THEN
        ALTER TABLE public.proposal_priority_agendas ADD CONSTRAINT uq_proposal_priority_agendas UNIQUE (proposal_id, agenda_key);
    END IF;
END $norm01_uq$;

COMMIT;

-- SECTION 5 - VERIFICATION (read after COMMIT)
SELECT 'NORM-01 fk present (expected 15)' AS check_name, count(*)::text AS value
  FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype='f' AND conname IN (
    'fk_activity_logs_user','fk_qpr_proposal','fk_qpr_created_by','fk_qpr_submitted_by',
    'fk_project_reports_proposal','fk_project_reports_submitted_by','fk_project_reports_reviewed_by',
    'fk_psr_reviewer','fk_proposals_proposal_announcement','fk_pau_actor',
    'fk_proposals_returned_by_user','fk_proposals_revision_forwarded_by_user','fk_ovcaf_released_by',
    'fk_dean_assignments_created_by','fk_dean_assignments_revoked_by')
UNION ALL
SELECT 'NORM-01 indexes present (expected 11)', count(*)::text FROM pg_indexes WHERE schemaname='public' AND indexname IN (
    'idx_qpr_created_by','idx_qpr_submitted_by','idx_project_reports_submitted_by','idx_project_reports_reviewed_by',
    'idx_psr_reviewer','idx_pau_actor','idx_proposals_returned_by','idx_proposals_revision_forwarded_by',
    'idx_ovcaf_released_by','idx_dean_assignments_created_by','idx_dean_assignments_revoked_by')
UNION ALL
SELECT 'NORM-01 unique present (expected 1)', count(*)::text FROM pg_constraint
 WHERE connamespace='public'::regnamespace AND conname='uq_proposal_priority_agendas'
UNION ALL
SELECT 'schema totals (informational)',
       (SELECT count(*)::text FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype='f') || ' fk / ' ||
       (SELECT count(*)::text FROM pg_indexes WHERE schemaname='public') || ' index / ' ||
       (SELECT count(*)::text FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE') || ' tables';
