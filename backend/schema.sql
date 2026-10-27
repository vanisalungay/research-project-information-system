-- PostgreSQL Database Schema for Research Project Information System (RPIS)

-- Drop tables if they exist to start fresh (useful for development/testing)
DROP TABLE IF EXISTS quarterly_progress_reports CASCADE;
DROP TABLE IF EXISTS project_reports CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS proposal_reviews CASCADE;
DROP TABLE IF EXISTS proposal_section_remarks CASCADE;
DROP TABLE IF EXISTS proposal_limitations CASCADE;
DROP TABLE IF EXISTS priority_agendas CASCADE;
DROP TABLE IF EXISTS priority_agenda_sets CASCADE;
DROP TABLE IF EXISTS proposal_priority_agendas CASCADE;
DROP TABLE IF EXISTS proposal_other_projects CASCADE;
DROP TABLE IF EXISTS proposal_budget CASCADE;
DROP TABLE IF EXISTS proposal_personnel CASCADE;
DROP TABLE IF EXISTS proposal_log_frames CASCADE;
DROP TABLE IF EXISTS proposal_sites CASCADE;
DROP TABLE IF EXISTS proposal_versions CASCADE;
DROP TABLE IF EXISTS proposal_cooperating_agencies CASCADE;
DROP TABLE IF EXISTS cooperating_agencies CASCADE;
DROP TABLE IF EXISTS proposals CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- 2. Proposals Table
CREATE TABLE proposals (
    id BIGSERIAL PRIMARY KEY,
    proponent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    proposal_announcement_id BIGINT,          -- announcement (submission window) this proposal was submitted under
    -- Proposal ID and Revision tracking
    proposal_code VARCHAR(10) UNIQUE,     -- 3-digit continuous counter: '001', '002', etc.
    revision_number INT DEFAULT 0,        -- 0 = initial submission, 1 = first revision, etc.
    document_id VARCHAR(100) UNIQUE,      -- Full format: MSUN-ORPS-DA-001-2026-REV00
    program_title VARCHAR(255),
    project_title VARCHAR(255) NOT NULL,
    project_leader VARCHAR(255),
    duration VARCHAR(50),
    start_date DATE,
    end_date DATE,
    college VARCHAR(255),
    address TEXT,
    -- Structured address + contact of the Create Proposal form's "Address *" and
    -- "Contact *" sections (section 01). `address` above keeps the combined
    -- "Address / Contact" text so every existing screen keeps working.
    address_country VARCHAR(120),
    address_region VARCHAR(120),
    address_province VARCHAR(120),
    address_district VARCHAR(120),
    address_municipality VARCHAR(120),
    address_barangay VARCHAR(120),
    contact_type VARCHAR(30),
    contact_value VARCHAR(255),
    cooperating_agencies TEXT,
    research_type VARCHAR(50),
    innovation_goals TEXT,
    sector_relevance TEXT,
    sdg TEXT,                              -- JSON array of selected SDG names
    executive_summary TEXT,
    rationale TEXT,
    framework TEXT,
    objectives_general TEXT,
    objectives_specific TEXT,
    review TEXT,
    review_file_name VARCHAR(255),
    methodology TEXT,
    roadmap_file_name VARCHAR(255),
    roadmap_file_path VARCHAR(500),         -- stored Technology Map image, relative to "uploads"
    technology_trl VARCHAR(50),
    outputs TEXT,
    outcomes TEXT,
    impact_economic TEXT,
    impact_social TEXT,
    beneficiaries TEXT,
    beneficiaries_file_name VARCHAR(255),
    beneficiaries_file_path VARCHAR(500),   -- stored Line-Item Budget file, relative to "uploads"
    sustainability TEXT,
    gad_score DECIMAL(5,2),
    gad_file_name VARCHAR(255),
    risks TEXT,
    references_text TEXT,
    references_json TEXT,                   -- structured entries behind the literature cited (JSON)
    other_projects_number VARCHAR(50),
    status VARCHAR(50) DEFAULT 'DRAFT',
    reviewed_by VARCHAR(255),
    reviewed_by_position VARCHAR(50),
    remarks TEXT,
    project_leader_sex VARCHAR(20),         -- Sex of the project leader (Project Profile)
    limitations_text TEXT,                  -- Free-text Limitations of the Project (section 20)
    -- Special Order (SO) fields issued by Office of the Chancellor
    so_number VARCHAR(100),
    so_file_name VARCHAR(255),
    so_file_path VARCHAR(500),
    so_uploaded_at TIMESTAMP,
    needs_budget BOOLEAN,                    -- Whether the proposal requires OVCAF funding step
    -- Return-for-Revision routing metadata
    returned_by_office VARCHAR(50),
    returned_by_name VARCHAR(255),
    returned_by_user_id BIGINT,
    returned_at TIMESTAMP,
    return_remarks TEXT,
    revision_deadline TIMESTAMP,
    revision_forwarded_at TIMESTAMP,
    revision_forwarded_by_name VARCHAR(255),
    revision_forwarded_by_user_id BIGINT,
    revision_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Proposal code allocator. nextval() is atomic, so simultaneous submissions
-- (from any number of backend instances) always receive a distinct, increasing
-- 3-digit code. The sequence never resets across years and is aligned with the
-- highest existing code at application startup (ProposalCodeSequenceInitializer).
CREATE SEQUENCE IF NOT EXISTS proposal_code_seq;

-- 3. Proposal Sites Table
CREATE TABLE proposal_sites (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    country VARCHAR(100),
    region VARCHAR(100),
    province VARCHAR(100),
    district VARCHAR(100),
    municipality VARCHAR(100),
    barangay VARCHAR(100)
);

-- 4. Proposal Log Frames Table
CREATE TABLE proposal_log_frames (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    outcome TEXT,
    output TEXT
);

-- 5. Proposal Personnel Table
CREATE TABLE proposal_personnel (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    position VARCHAR(100),
    time_devoted VARCHAR(50),
    responsibilities TEXT
);

-- 6. Proposal Budget Table
CREATE TABLE proposal_budget (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    agency VARCHAR(255),
    ps VARCHAR(50),
    mooe VARCHAR(50),
    eo VARCHAR(50),
    total VARCHAR(50)
);

-- 7. Proposal Other Projects Table
CREATE TABLE proposal_other_projects (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    title VARCHAR(255),
    agency VARCHAR(255),
    involvement VARCHAR(255)
);

-- 8. Proposal Priority Agendas Table
CREATE TABLE proposal_priority_agendas (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    agenda_key VARCHAR(50) NOT NULL,
    selected BOOLEAN DEFAULT FALSE,
    value VARCHAR(255)
);

-- 8b. Priority Agenda framework (agenda set) and its agenda items.
-- Section 05 of the Create Proposal form used to hard-code the framework label
-- ("based on MSUN RIIDE 2025-2028") and the list of five agenda programs. Both are
-- master data now, maintained by an administrator. Exactly one set may be active
-- for new submissions (partial unique index uk_priority_agenda_set_single_active);
-- sets and items are only deactivated, never deleted, and agenda_key is immutable
-- because it is what proposal_priority_agendas.agenda_key stores.
CREATE TABLE priority_agenda_sets (
    id BIGSERIAL PRIMARY KEY,
    framework_name VARCHAR(150) NOT NULL,
    coverage_start_year INT NOT NULL,
    coverage_end_year INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_priority_agenda_period CHECK (coverage_end_year >= coverage_start_year)
);

CREATE UNIQUE INDEX uk_priority_agenda_set_framework_period
    ON priority_agenda_sets (LOWER(framework_name), coverage_start_year, coverage_end_year);
CREATE UNIQUE INDEX uk_priority_agenda_set_single_active
    ON priority_agenda_sets (active) WHERE active = TRUE;

CREATE TABLE priority_agendas (
    id BIGSERIAL PRIMARY KEY,
    agenda_set_id BIGINT NOT NULL REFERENCES priority_agenda_sets(id),
    agenda_key VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    sub_label VARCHAR(80),
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_priority_agendas_set_key ON priority_agendas (agenda_set_id, LOWER(agenda_key));
CREATE UNIQUE INDEX uk_priority_agendas_set_name_lower ON priority_agendas (agenda_set_id, LOWER(name));
CREATE INDEX idx_priority_agendas_set_active ON priority_agendas (agenda_set_id, active, display_order);

-- Seed the framework the form used to hard-code (PriorityAgendaInitializer does the
-- same idempotently on startup, so the feature works on an existing database too).
INSERT INTO priority_agenda_sets (framework_name, coverage_start_year, coverage_end_year, active)
VALUES ('MSUN RIIDE', 2025, 2028, TRUE);

INSERT INTO priority_agendas (agenda_set_id, agenda_key, name, sub_label, description, display_order)
SELECT s.id, a.agenda_key, a.name, a.sub_label, a.description, a.display_order
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
  AND s.coverage_end_year = 2028;

-- 9. Proposal Limitations Table
CREATE TABLE proposal_limitations (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    limitation TEXT,
    remarks TEXT
);

-- 10. Proposal Reviews Table
CREATE TABLE proposal_reviews (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    reviewer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewer_role VARCHAR(50),
    -- Soundness of Proposal (20%)
    soundness_sector_need_score INT,
    soundness_effective_solution_score INT,
    soundness_reasonable_budget_score INT,
    soundness_doable_workplan_score INT,
    -- Suitability of Output (30%)
    suitability_cost_effective_score INT,
    suitability_partners_identified_score INT,
    suitability_commercialization_score INT,
    suitability_timely_utilization_score INT,
    -- Significance of Outcome (30%)
    significance_economic_impact_score INT,
    significance_social_impact_score INT,
    significance_environmental_impact_score INT,
    significance_sustainability_score INT,
    -- Competence of Proponent (20%)
    competence_alignment_score INT,
    competence_collaboration_score INT,
    competence_deliverables_understanding_score INT,
    -- Overall comments and decision
    overall_comments TEXT,
    decision VARCHAR(50), -- e.g., 'APPROVED', 'REJECTED', 'REVISION_REQUIRED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 11. Notifications Table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    proposal_id BIGINT,
    -- Call for Proposals (proposal announcement) this notification refers to, when the
    -- notification was raised for an announcement instead of for a proposal. Nullable:
    -- every other notification keeps NULL and simply has no announcement to open.
    proposal_announcement_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 12. Project Reports Table (Quarterly Progress and Financial Reports)
CREATE TABLE project_reports (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT REFERENCES proposals(id) ON DELETE CASCADE,
    report_type VARCHAR(50) NOT NULL, -- QUARTERLY_PROGRESS, FINANCIAL
    period VARCHAR(20) NOT NULL, -- Q1, Q2, Q3, Q4, FINAL
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    remarks VARCHAR(1000),
    submitted_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    review_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, RETURNED
    review_comments VARCHAR(1000)
);

-- 13. OVCAF Validations Table (Financial validation and budget authorization)
CREATE TABLE ovcaf_validations (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    validated_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    validated_at TIMESTAMP,
    decision VARCHAR(50) NOT NULL, -- APPROVED_ENDORSED, RETURNED_FOR_REVISION, REJECTED
    remarks TEXT,
    -- Validation Checklist
    budget_complete BOOLEAN DEFAULT FALSE,
    budget_computation_correct BOOLEAN DEFAULT FALSE,
    supporting_docs_complete BOOLEAN DEFAULT FALSE,
    expenses_reasonable BOOLEAN DEFAULT FALSE,
    funding_source_identified BOOLEAN DEFAULT FALSE,
    compliance_verified BOOLEAN DEFAULT FALSE,
    -- Budget authorization fields
    budget_endorsed BOOLEAN DEFAULT FALSE,
    budget_endorsed_at TIMESTAMP,
    budget_authorized BOOLEAN DEFAULT FALSE,
    budget_authorized_at TIMESTAMP,
    funds_released BOOLEAN DEFAULT FALSE,
    funds_released_at TIMESTAMP,
    released_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 14. Proposal Announcements Table (submission windows for new proposals)
CREATE TABLE proposal_announcements (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT | ACTIVE | CLOSED
    -- RIIDE priority-agenda program keys and standard reminder ids selected for
    -- this call (JSON arrays of stable identifiers, e.g. ["dagat","punla"] and
    -- ["research-plan","collaboration"]), plus the optional announcement body.
    priority_agendas TEXT,
    reminder_ids TEXT,
    additional_announcement TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_announcement_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_announcement_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
);

-- 15. Proposal Versions Table (immutable submission & revision history)
CREATE TABLE proposal_versions (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    document_id VARCHAR(100),
    status VARCHAR(50),
    submitted_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    submitted_by_name VARCHAR(255),
    submission_date TIMESTAMP,
    revision_date TIMESTAMP,
    revision_remarks TEXT,
    details_json TEXT,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_proposal_versions UNIQUE (proposal_id, version_number)
);

-- 16. Proposal Section Remarks Table (section- AND field-level revision feedback)
-- One row per reviewer remark, bound to exactly one proposal, one numbered
-- proposal section and (optionally) one specific field of that section. Remarks are
-- grouped into revision rounds: a new review cycle opens a new round (previous
-- ACTIVE rows become SUPERSEDED so nothing is overwritten).
CREATE TABLE proposal_section_remarks (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    section_key VARCHAR(10) NOT NULL,              -- canonical "01".."27"
    section_label VARCHAR(150),
    field_key VARCHAR(120),                        -- stable field path; NULL = section-level
    field_label VARCHAR(255),
    entry_index INT,                               -- position of the repeated entry (last resort)
    entry_id BIGINT,                               -- best-effort stable id of the repeated entry
    entry_snapshot TEXT,                           -- JSON snapshot of the entry as reviewed
    remark TEXT NOT NULL,
    round INT NOT NULL,                            -- 1-based revision round
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SUPERSEDED
    reviewer_role VARCHAR(50),
    reviewer_name VARCHAR(255),
    reviewer_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 17. Quarterly Progress Reports Table (implementation-phase reporting for
-- approved, SO-issued projects)
CREATE TABLE quarterly_progress_reports (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    period VARCHAR(10) NOT NULL, -- Q1, Q2, Q3, Q4, TERMINAL
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED
    report_type VARCHAR(30) NOT NULL DEFAULT 'QUARTERLY_PROGRESS', -- QUARTERLY_PROGRESS | TERMINAL_REPORT
    project_title VARCHAR(255),
    project_leader VARCHAR(255),
    leader_gender VARCHAR(20),
    base_station TEXT,
    sites_of_implementation TEXT,
    project_duration VARCHAR(50),
    project_start_date DATE,
    project_end_date DATE,
    catch_up_plan TEXT,
    problems_concerns TEXT,
    suggested_solutions TEXT,
    objectives_json TEXT,
    outputs_json TEXT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    submitted_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    -- Reporting-stage documents (stages 1-3 require a Project Workplan; the final
    -- stage requires a Terminal Report). Stored under the uploads/reports convention.
    workplan_file_name VARCHAR(255),
    workplan_file_path VARCHAR(500),
    terminal_report_file_name VARCHAR(255),
    terminal_report_file_path VARCHAR(500),
    -- Structured interactive Project Workplan (stages 1-3): objectives with
    -- monthly target activities/accomplishments, totals, remarks, "As of" date.
    -- Legacy uploaded workplan files above are preserved for old reports.
    workplan_json TEXT,
    -- Structured Terminal Report (stage 4) completed in RPIS: title page, summary
    -- sheet, abstract, acknowledgment, table of contents and lists, text sections,
    -- literature cited, appendices, problems, outputs and utilization. Legacy
    -- uploaded terminal report files above are preserved for old reports.
    terminal_report_json TEXT,
    -- Report this one was started from through "Create a Copy" (traceability only; the
    -- source report is never modified). NULL for every report written from scratch.
    copied_from_report_id BIGINT
);

-- 18. Cooperating Agencies Table (shared directory of section 02 of the Create
-- Proposal form). One row per agency, reusable by every proposal. The
-- case-insensitive unique index is the database-level guarantee that the same
-- agency cannot be stored twice; it is also created idempotently on startup by
-- CooperatingAgencyInitializer, which seeds the 21 default agencies (and never
-- inserts a seed record twice).
CREATE TABLE cooperating_agencies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_cooperating_agencies_name_lower ON cooperating_agencies (LOWER(name));

-- 19. Proposal Cooperating Agencies Table (join table). One row per agency
-- selected on one proposal, so a proposal can name any number of agencies and an
-- agency is reused by every proposal that selects it. Rows are replaced when the
-- proposal is saved and removed when the proposal is deleted; the directory itself
-- is never touched by either operation.
CREATE TABLE proposal_cooperating_agencies (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    cooperating_agency_id BIGINT NOT NULL REFERENCES cooperating_agencies(id),
    CONSTRAINT uk_proposal_cooperating_agency UNIQUE (proposal_id, cooperating_agency_id)
);

CREATE INDEX idx_proposal_cooperating_agencies_proposal ON proposal_cooperating_agencies (proposal_id);

-- 20. Proposal Announcement Updates Table (Call for Proposal update history).
-- A Call for Proposal keeps only its current values, so every significant,
-- public-facing change is appended here instead of overwriting the previous
-- values. This lets the Proponent Side review the original deadline and each
-- announced change (for example a deadline extension) in chronological order.
-- The foreign key cascades, so deleting a call removes its history too.
CREATE TABLE proposal_announcement_updates (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES proposal_announcements(id) ON DELETE CASCADE,
    update_type VARCHAR(30) NOT NULL, -- PUBLISHED | DEADLINE_EXTENDED | DEADLINE_CHANGED | STATUS_CHANGED | DETAILS_UPDATED
    previous_status VARCHAR(20),
    previous_start_date DATE,
    previous_end_date DATE,
    new_status VARCHAR(20),
    new_start_date DATE,
    new_end_date DATE,
    effective_date DATE,
    -- Optional public-facing announcement/explanation provided by the administrator.
    explanation TEXT,
    -- Administrator/source responsible for the change, when available.
    actor_id BIGINT,
    actor_name VARCHAR(255),
    actor_role VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_proposal_announcement_updates_announcement
    ON proposal_announcement_updates (announcement_id, created_at);

