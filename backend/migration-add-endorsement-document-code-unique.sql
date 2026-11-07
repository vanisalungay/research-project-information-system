-- Migration: guarantee a unique Document Code per issued endorsement letter
-- ---------------------------------------------------------------------------
-- The issued endorsement letter prints a Document Code (letter_document_code) and one code
-- must never be issued for two endorsements. The constraint is declared by the original
-- endorsement migration and on the entity, but a database provisioned by Hibernate
-- (ddl-auto=update) instead of this migration set can be missing it, so it is re-asserted
-- here. Idempotent and additive: it never renumbers and never changes an existing code.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_endorsement_requests_document_code'
    ) THEN
        ALTER TABLE endorsement_requests
            ADD CONSTRAINT uk_endorsement_requests_document_code UNIQUE (letter_document_code);
    END IF;
END $$;

-- No data change: every existing endorsement keeps the code it was issued with.
