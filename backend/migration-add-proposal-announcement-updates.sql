-- Migration: Call for Proposal update history
-- ---------------------------------------------------------------------------
-- A Call for Proposal (proposal_announcements) keeps only its CURRENT values, so
-- a deadline extension would otherwise erase the original deadline. This table
-- records the significant, public-facing changes an administrator makes, so the
-- Proponent Side can review the original deadline and every announced change in
-- chronological order.
--
--   announcement_id    The proposal_announcements.id the change belongs to. A
--                      real foreign key (ON DELETE CASCADE) so the history
--                      follows its call and is never orphaned.
--   update_type        PUBLISHED | DEADLINE_EXTENDED | DEADLINE_CHANGED |
--                      STATUS_CHANGED | DETAILS_UPDATED
--   previous_*         The values before the change (NULL for the first
--                      publication, which has nothing before it).
--   new_*              The values after the change.
--   effective_date     The day the change was announced / takes effect.
--   explanation        Optional public-facing explanation provided by the
--                      administrator (for example why a deadline was extended).
--   actor_*            The administrator/source responsible, when available.
--   created_at         When the change was recorded.
--
-- Only genuine changes are recorded; nothing is fabricated for calls that
-- existed before this table was introduced.
--
-- Safe to run on existing databases: the table is additive, so existing calls
-- simply have no history until they are next updated.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate also creates this
-- table automatically; this script documents/keeps the DDL explicit.

CREATE TABLE IF NOT EXISTS proposal_announcement_updates (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES proposal_announcements(id) ON DELETE CASCADE,
    update_type VARCHAR(30) NOT NULL,
    previous_status VARCHAR(20),
    previous_start_date DATE,
    previous_end_date DATE,
    new_status VARCHAR(20),
    new_start_date DATE,
    new_end_date DATE,
    effective_date DATE,
    explanation TEXT,
    actor_id BIGINT,
    actor_name VARCHAR(255),
    actor_role VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_proposal_announcement_updates_announcement
    ON proposal_announcement_updates (announcement_id, created_at);
