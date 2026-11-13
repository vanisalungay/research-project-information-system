-- Migration: Proposal Announcements table
-- A Proposal Announcement defines the date window (start date to end date) during
-- which proponents are allowed to submit new proposals.
-- Note: with spring.jpa.hibernate.ddl-auto=update Hibernate will also create
-- this table automatically; this script documents/keeps the DDL explicit.

CREATE TABLE IF NOT EXISTS proposal_announcements (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT | ACTIVE | CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_announcement_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_announcement_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_proposal_announcements_status ON proposal_announcements (status);
CREATE INDEX IF NOT EXISTS idx_proposal_announcements_dates ON proposal_announcements (start_date, end_date);
