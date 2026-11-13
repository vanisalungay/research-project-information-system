-- Migration: Quarterly Progress Reports (structured in-system reporting)
-- ---------------------------------------------------------------------------
-- Adds the quarterly_progress_reports table used to store the digital
-- "MSUN Internally-Funded Project Quarterly Progress Report" form. Only
-- approved, SO-issued projects are eligible to have these reports.
CREATE TABLE IF NOT EXISTS quarterly_progress_reports (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    period VARCHAR(10) NOT NULL, -- Q1, Q2, Q3, Q4
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED
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
    submitted_at TIMESTAMP
);

-- Index for fast lookup of a proposal's quarterly report history.
CREATE INDEX IF NOT EXISTS idx_qpr_proposal ON quarterly_progress_reports(proposal_id);
