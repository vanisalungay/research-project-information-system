-- Migration: dynamic, admin-managed Priority Agendas (section 05 of the Create
-- Proposal form)
-- ---------------------------------------------------------------------------
-- The Proponent "Submit New Proposal" form used to hard-code the framework label
-- ("based on MSUN RIIDE 2025-2028") and the list of five agenda programs in the
-- Vue components. Both are master data now:
--
--   priority_agenda_sets   the framework / agenda set: the framework or program
--                          name plus the coverage period (start and end year).
--   priority_agendas       the agenda items of a set. `agenda_key` is the stable
--                          identifier persisted with every proposal in
--                          proposal_priority_agendas.agenda_key, so it is
--                          immutable once created.
--
-- The existing proposal_priority_agendas table is NOT touched: a proposal keeps
-- storing agenda_key / selected / value exactly as before, so every historical
-- submission keeps its selection - including a selection whose agenda is later
-- deactivated. Nothing is deleted or renumbered.
--
-- Exactly one set may be active for new submissions. That is enforced at the
-- database level by the partial unique index
-- uk_priority_agenda_set_single_active, so two conflicting sets can never be
-- active at the same time, not even under concurrent requests.
--
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate creates the two tables
-- from the entities, but it does not create the partial unique index, the
-- case-insensitive unique indexes or the CHECK constraint. PriorityAgendaInitializer
-- creates all of them idempotently on startup and seeds the default framework;
-- this script documents/keeps the DDL explicit.

CREATE TABLE IF NOT EXISTS priority_agenda_sets (
    id BIGSERIAL PRIMARY KEY,
    framework_name VARCHAR(150) NOT NULL,
    coverage_start_year INT NOT NULL,
    coverage_end_year INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- An invalid coverage period can never be stored.
    CONSTRAINT chk_priority_agenda_period CHECK (coverage_end_year >= coverage_start_year)
);

-- The same framework and coverage period cannot be configured twice.
CREATE UNIQUE INDEX IF NOT EXISTS uk_priority_agenda_set_framework_period
    ON priority_agenda_sets (LOWER(framework_name), coverage_start_year, coverage_end_year);

-- At most ONE set may be active for new submissions.
CREATE UNIQUE INDEX IF NOT EXISTS uk_priority_agenda_set_single_active
    ON priority_agenda_sets (active) WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS priority_agendas (
    id BIGSERIAL PRIMARY KEY,
    agenda_set_id BIGINT NOT NULL REFERENCES priority_agenda_sets(id),
    -- Stable identifier persisted with every proposal; never edited after creation.
    -- Same length as proposal_priority_agendas.agenda_key so it always fits.
    agenda_key VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    sub_label VARCHAR(80),
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- The same stable key and the same display name cannot repeat inside one set.
CREATE UNIQUE INDEX IF NOT EXISTS uk_priority_agendas_set_key
    ON priority_agendas (agenda_set_id, LOWER(agenda_key));
CREATE UNIQUE INDEX IF NOT EXISTS uk_priority_agendas_set_name_lower
    ON priority_agendas (agenda_set_id, LOWER(name));

CREATE INDEX IF NOT EXISTS idx_priority_agendas_set_active
    ON priority_agendas (agenda_set_id, active, display_order);

-- Seed the framework the form used to hard-code, so the feature works out of the
-- box and every existing agenda key keeps resolving. Inserted only when absent, so
-- re-running this script never duplicates the rows and never overwrites a set an
-- administrator has since edited or deactivated.
INSERT INTO priority_agenda_sets
    (framework_name, coverage_start_year, coverage_end_year, active, created_at, updated_at)
SELECT 'MSUN RIIDE', 2025, 2028, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM priority_agenda_sets
    WHERE LOWER(framework_name) = LOWER('MSUN RIIDE')
      AND coverage_start_year = 2025
      AND coverage_end_year = 2028
);

INSERT INTO priority_agendas
    (agenda_set_id, agenda_key, name, sub_label, description, display_order, active, created_at, updated_at)
SELECT s.id, a.agenda_key, a.name, a.sub_label, a.description, a.display_order, TRUE,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM priority_agenda_sets s
CROSS JOIN (VALUES
    ('dagat', 'DAGAT Program', 'Commodity',
     'Development of Aquatic Governance and Advanced Technologies', 1),
    ('punla', 'PUNLA Program', 'Priority Topic',
     'Progressive Upliftment for Nutrition, Livelihood, and Agriculture', 2),
    ('kalikasan', 'KALIKASAN Program', 'Sector',
     'Knowledge and Actions for Lasting Impact: Keeping Atmospheres, Seas, and Nature Sustainable', 3),
    ('negosyo', 'NEGOSYO Program', 'Sector',
     'Nurturing Entrepreneurship and Growth through Outstanding Strategic Yielding Opportunities', 4),
    ('tanglaw', 'TANGLAW Program', 'Sector',
     'Transforming Academia through Nurturing Growth, Leadership, Advancement, and Wisdom', 5)
) AS a(agenda_key, name, sub_label, description, display_order)
WHERE LOWER(s.framework_name) = LOWER('MSUN RIIDE')
  AND s.coverage_start_year = 2025
  AND s.coverage_end_year = 2028
ON CONFLICT DO NOTHING;
