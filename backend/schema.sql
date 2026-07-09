-- PostgreSQL Database Schema for Research Project Information System (RPIS)

-- Drop tables if they exist to start fresh (useful for development/testing)
DROP TABLE IF EXISTS project_reports CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS proposal_reviews CASCADE;
DROP TABLE IF EXISTS proposal_limitations CASCADE;
DROP TABLE IF EXISTS proposal_priority_agendas CASCADE;
DROP TABLE IF EXISTS proposal_other_projects CASCADE;
DROP TABLE IF EXISTS proposal_budget CASCADE;
DROP TABLE IF EXISTS proposal_personnel CASCADE;
DROP TABLE IF EXISTS proposal_log_frames CASCADE;
DROP TABLE IF EXISTS proposal_sites CASCADE;
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
    program_title VARCHAR(255),
    project_title VARCHAR(255) NOT NULL,
    project_leader VARCHAR(255),
    duration VARCHAR(50),
    start_date DATE,
    end_date DATE,
    college VARCHAR(255),
    address TEXT,
    cooperating_agencies TEXT,
    research_type VARCHAR(50),
    innovation_goals TEXT,
    sector_relevance TEXT,
    sdg TEXT,
    executive_summary TEXT,
    rationale TEXT,
    framework TEXT,
    objectives_general TEXT,
    objectives_specific TEXT,
    review TEXT,
    review_file_name VARCHAR(255),
    methodology TEXT,
    roadmap_file_name VARCHAR(255),
    technology_trl VARCHAR(50),
    outputs TEXT,
    outcomes TEXT,
    impact_economic TEXT,
    impact_social TEXT,
    beneficiaries TEXT,
    beneficiaries_file_name VARCHAR(255),
    sustainability TEXT,
    gad_score DECIMAL(5,2),
    gad_file_name VARCHAR(255),
    risks TEXT,
    references_text TEXT,
    other_projects_number VARCHAR(50),
    status VARCHAR(50) DEFAULT 'DRAFT',
    reviewed_by VARCHAR(255),
    reviewed_by_position VARCHAR(50),
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
