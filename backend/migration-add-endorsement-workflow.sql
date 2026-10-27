-- Migration: Dean / Department Chair endorsement workflow
-- ---------------------------------------------------------------------------
-- Adds the endorsement request the Faculty Proponent raises before starting the
-- full proposal submission, the Dean's decision on it, the issued official
-- endorsement letter, the Certifications entry that makes that letter available
-- to the Proponent, and an append-only audit trail.
--
-- Every change is additive: no existing table, column or record is dropped,
-- renamed or rewritten. With spring.jpa.hibernate.ddl-auto=update Hibernate also
-- creates these structures; this script is the explicit, reviewable DDL and is the
-- place where the constraints and indexes the production database must carry are
-- declared (Hibernate does not reliably create every constraint).
--
-- Idempotent: safe to run repeatedly.

-- ---------------------------------------------------------------------------
-- 1. Dean profile information on the existing user account
-- ---------------------------------------------------------------------------
-- A Dean authenticates through the same users row as every other role, so the
-- account carries the two extra facts the endorsement workflow needs: the
-- designation/title printed on the letter ("Dean", "Department Chair") and the
-- college the Dean is authorized to endorse for. assigned_college is nullable:
-- NULL means "no college configured yet", which the service treats as the
-- institution-wide fallback (and records in the audit trail).
--
-- The authorized signature needs no new column: the existing electronic
-- signature of the account (signature_file_name / signature_file_path, see
-- migration-add-user-signature.sql) is reused.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS designation VARCHAR(120),
    ADD COLUMN IF NOT EXISTS assigned_college VARCHAR(255);

-- ---------------------------------------------------------------------------
-- 2. Endorsement requests
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS endorsement_requests (
    id BIGSERIAL PRIMARY KEY,

    -- Human-readable, unique reference allocated from endorsement_code_seq,
    -- format MSUN-ORPS-END-<code>-<year>.
    reference_number VARCHAR(60) NOT NULL,

    proponent_id BIGINT NOT NULL REFERENCES users(id),
    -- The Dean the request was routed to. NULL while no Dean is configured for the
    -- request's college (the request stays PENDING and any Dean may pick it up).
    assigned_dean_id BIGINT REFERENCES users(id),

    -- Preliminary information only: the Proponent does not fill in the full
    -- research proposal at this stage.
    college VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    proposal_title VARCHAR(500) NOT NULL,
    proposal_announcement_id BIGINT NOT NULL REFERENCES proposal_announcements(id),
    research_summary TEXT,
    notes TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Decision attribution: the authenticated Dean who approved or rejected.
    decided_by_user_id BIGINT REFERENCES users(id),
    decided_by_name VARCHAR(255),
    decided_by_designation VARCHAR(120),
    decided_at TIMESTAMP,
    endorsement_date DATE,
    rejection_reason TEXT,

    -- Issued official letter. letter_document_code is unique so one document code
    -- can never be issued twice, and letter_file_path points at the stored PDF
    -- under uploads/endorsements/. The signature is copied into that directory at
    -- approval time, so replacing the account signature later can never alter an
    -- already issued letter.
    letter_document_code VARCHAR(80),
    letter_file_name VARCHAR(255),
    letter_file_path VARCHAR(500),
    letter_version INT,
    letter_generated_at TIMESTAMP,
    signature_file_name VARCHAR(255),
    signature_file_path VARCHAR(500),

    -- The proposal the Proponent continued to after the endorsement, and the
    -- Certifications entry that carries the issued letter.
    proposal_id BIGINT REFERENCES proposals(id),
    certification_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_endorsement_requests_reference UNIQUE (reference_number),
    CONSTRAINT uk_endorsement_requests_document_code UNIQUE (letter_document_code),
    CONSTRAINT chk_endorsement_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    -- An approved request must carry the Dean attribution and the issued letter;
    -- a rejected one must carry the reason. Enforced by the database as well as
    -- by the service so a misleading row cannot be written from anywhere.
    CONSTRAINT chk_endorsement_requests_decision CHECK (
        (status = 'PENDING')
        OR (status = 'APPROVED' AND decided_by_user_id IS NOT NULL
            AND decided_at IS NOT NULL AND letter_file_path IS NOT NULL)
        OR (status = 'REJECTED' AND decided_by_user_id IS NOT NULL
            AND decided_at IS NOT NULL AND rejection_reason IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_endorsement_requests_proponent
    ON endorsement_requests (proponent_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_endorsement_requests_dean
    ON endorsement_requests (assigned_dean_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_endorsement_requests_status
    ON endorsement_requests (status);

-- ---------------------------------------------------------------------------
-- 3. Endorsement request audit trail (append-only)
-- ---------------------------------------------------------------------------
-- One row per significant event (created, assigned, approved, rejected, letter
-- generated, reissued/superseded, linked to a proposal, Dean role assigned or
-- revoked, Dean college changed). Rows are never updated or deleted, so previous
-- decisions stay readable exactly as the existing proposal_announcement_updates
-- history does.
CREATE TABLE IF NOT EXISTS endorsement_request_events (
    id BIGSERIAL PRIMARY KEY,
    -- NULL for account-level events (Dean role assigned/revoked, Dean college
    -- changed), which belong to no single request but must still be auditable.
    endorsement_request_id BIGINT REFERENCES endorsement_requests(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    actor_id BIGINT,
    actor_name VARCHAR(255),
    actor_role VARCHAR(50),
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_endorsement_request_events_request
    ON endorsement_request_events (endorsement_request_id, created_at);

-- ---------------------------------------------------------------------------
-- 4. Certifications
-- ---------------------------------------------------------------------------
-- The Proponent's Certifications section. The RPIS had no certification record
-- before this feature, so the smallest suitable structure is introduced here:
-- one row per issued document, owned by the Proponent it was issued to.
-- endorsement_request_id is UNIQUE, which is the database-level guarantee that a
-- retried approval can never produce a second certification for the same letter.
CREATE TABLE IF NOT EXISTS certifications (
    id BIGSERIAL PRIMARY KEY,
    proponent_id BIGINT NOT NULL REFERENCES users(id),
    endorsement_request_id BIGINT REFERENCES endorsement_requests(id),
    proposal_id BIGINT REFERENCES proposals(id),
    document_type VARCHAR(40) NOT NULL DEFAULT 'ENDORSEMENT_LETTER',
    title VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    endorsement_date DATE,
    issuing_dean_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_certifications_endorsement_request UNIQUE (endorsement_request_id),
    CONSTRAINT chk_certifications_status CHECK (status IN ('ISSUED', 'SUPERSEDED'))
);

CREATE INDEX IF NOT EXISTS idx_certifications_proponent
    ON certifications (proponent_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_certifications_type
    ON certifications (document_type);

-- Link endorsement_requests.certification_id -> certifications(id). Added after
-- both tables exist (the two tables reference each other).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_endorsement_requests_certification') THEN
        ALTER TABLE endorsement_requests
            ADD CONSTRAINT fk_endorsement_requests_certification
            FOREIGN KEY (certification_id) REFERENCES certifications(id);
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 5. Stable link from the proposal to the endorsement it was submitted under
-- ---------------------------------------------------------------------------
-- The association is a foreign key, never a title match, so two proposals with
-- the same title can never be confused. Nullable: a proposal written without an
-- endorsement keeps working exactly as before (the current application does not
-- require an endorsement before submission).
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS endorsement_request_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_proposals_endorsement_request') THEN
        ALTER TABLE proposals
            ADD CONSTRAINT fk_proposals_endorsement_request
            FOREIGN KEY (endorsement_request_id) REFERENCES endorsement_requests(id);
    END IF;
END $$;

-- A single endorsement may back at most one proposal.
CREATE UNIQUE INDEX IF NOT EXISTS uk_proposals_endorsement_request
    ON proposals (endorsement_request_id)
    WHERE endorsement_request_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 6. Endorsement letter settings (single row)
-- ---------------------------------------------------------------------------
-- The official letter names the ORPS Director it is addressed to, the office and
-- the university. The university name and the office name are printed on the
-- official template itself and the university name is already used throughout
-- RPIS, but the DIRECTOR'S NAME AND SURNAME ARE NOT AVAILABLE ANYWHERE IN THE
-- REPOSITORY. They are therefore an authorized administrative setting instead of a
-- hardcoded value: an RPS_ADMIN fills them in once (Endorsement Letter Settings)
-- and the endorsement letter is refused until they are supplied, so no letter is
-- ever issued to a fabricated or placeholder recipient.
--
-- A single row (id = 1) holds the settings; the application creates it on first
-- read and an optional rpis.endorsement.* property can seed it on a fresh
-- deployment.
CREATE TABLE IF NOT EXISTS endorsement_letter_settings (
    id BIGINT PRIMARY KEY,
    director_name VARCHAR(255),
    director_surname VARCHAR(120),
    director_designation VARCHAR(160) NOT NULL DEFAULT 'Director',
    office_name VARCHAR(255) NOT NULL DEFAULT 'Office of Research and Publication Services (ORPS)',
    university_name VARCHAR(255) NOT NULL DEFAULT 'Mindanao State University at Naawan',
    updated_by_user_id BIGINT REFERENCES users(id),
    updated_by_name VARCHAR(255),
    updated_at TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- 7. Concurrency-safe endorsement reference allocation
-- ---------------------------------------------------------------------------
-- Same mechanism as proposal_code_seq: nextval() is atomic, so simultaneous
-- requests from any number of backend instances always receive a distinct
-- reference. Existing records are never renumbered.
CREATE SEQUENCE IF NOT EXISTS endorsement_code_seq;

SELECT setval(
    'endorsement_code_seq',
    GREATEST(
        (SELECT COALESCE(MAX(CAST(substring(reference_number FROM 'END-([0-9]+)-') AS INTEGER)), 1)
           FROM endorsement_requests
          WHERE reference_number ~ '^MSUN-ORPS-END-[0-9]+-[0-9]{4}$'),
        1
    ),
    (SELECT COUNT(*) > 0 FROM endorsement_requests
      WHERE reference_number ~ '^MSUN-ORPS-END-[0-9]+-[0-9]{4}$')
);

CREATE INDEX IF NOT EXISTS idx_endorsement_requests_announcement
    ON endorsement_requests (proposal_announcement_id);
CREATE INDEX IF NOT EXISTS idx_endorsement_requests_college
    ON endorsement_requests (college);
