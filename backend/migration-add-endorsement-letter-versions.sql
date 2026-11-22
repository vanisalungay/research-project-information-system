-- =============================================================================
-- RPIS migration: preserve every version of an issued endorsement letter
-- =============================================================================
-- A reissue has always written a new PDF and left the superseded file in place, but
-- only the current file was reachable: endorsement_requests carries one
-- letter_file_path, and the old path existed only inside the LETTER_REISSUED audit
-- text. This migration adds the structured history the Dean's review screen needs.
--
-- Additive and idempotent, like every other migration-*.sql:
--   * the table is created only when it does not exist;
--   * the current letter of every approved request is back-filled once;
--   * a superseded version named by a LETTER_REISSUED event is back-filled once.
--
-- Existing data limitation: a reissued request whose LETTER_REISSUED event does not
-- name the superseded path cannot have that file attributed to it. Its current version
-- is still back-filled, and every reissue from now on records both files.
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

CREATE TABLE IF NOT EXISTS endorsement_letter_versions (
    id                     BIGSERIAL    PRIMARY KEY,
    endorsement_request_id BIGINT       NOT NULL REFERENCES endorsement_requests(id),
    version_number         INT          NOT NULL,
    document_code          VARCHAR(255),
    file_name              VARCHAR(255),
    file_path              VARCHAR(500) NOT NULL,
    generated_at           TIMESTAMP,
    generated_by_user_id   BIGINT,
    generated_by_name      VARCHAR(255),
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_endorsement_letter_versions_request_version
    ON endorsement_letter_versions (endorsement_request_id, version_number);

CREATE INDEX IF NOT EXISTS idx_endorsement_letter_versions_request
    ON endorsement_letter_versions (endorsement_request_id, version_number DESC);

-- A letter approved before the version column existed has no version number. It is the
-- original version, so record that on the request itself; the history row below and the
-- "current" flag both read this column.
UPDATE endorsement_requests
   SET letter_version = 1
 WHERE letter_version IS NULL
   AND letter_file_path IS NOT NULL
   AND status = 'APPROVED';

-- Back-fill the current letter of every approved request (version 1 for a letter that
-- was never reissued, the current number otherwise).
INSERT INTO endorsement_letter_versions
    (endorsement_request_id, version_number, document_code, file_name, file_path,
     generated_at, generated_by_user_id, generated_by_name)
SELECT r.id,
       COALESCE(r.letter_version, 1),
       r.letter_document_code,
       r.letter_file_name,
       r.letter_file_path,
       COALESCE(r.letter_generated_at, r.decided_at),
       r.decided_by_user_id,
       r.decided_by_name
  FROM endorsement_requests r
 WHERE r.letter_file_path IS NOT NULL
   AND r.status = 'APPROVED'
ON CONFLICT (endorsement_request_id, version_number) DO NOTHING;

-- Recover the superseded versions whose stored path the reissue audit entry names.
INSERT INTO endorsement_letter_versions
    (endorsement_request_id, version_number, document_code, file_name, file_path,
     generated_at)
SELECT DISTINCT
       e.endorsement_request_id,
       (regexp_match(e.detail,
              'Superseded version ([0-9]+) remains stored at ([^ ]+)\.'))[1]::int,
       r.letter_document_code,
       'endorsement-letter.pdf',
       (regexp_match(e.detail,
              'Superseded version ([0-9]+) remains stored at ([^ ]+)\.'))[2],
       e.created_at
  FROM endorsement_request_events e
  JOIN endorsement_requests r ON r.id = e.endorsement_request_id
 WHERE e.event_type = 'LETTER_REISSUED'
   AND e.detail ~ 'Superseded version [0-9]+ remains stored at [^ ]+\.'
ON CONFLICT (endorsement_request_id, version_number) DO NOTHING;

COMMIT;

-- ===========================================================================
-- VERIFICATION (read-only)
-- ===========================================================================
SELECT 'letter version rows' AS check_name, count(*)::text AS value
  FROM endorsement_letter_versions
UNION ALL
SELECT 'approved letters with a current version row',
       count(*)::text
  FROM endorsement_requests r
 WHERE r.letter_file_path IS NOT NULL
   AND r.status = 'APPROVED'
   AND EXISTS (SELECT 1
                 FROM endorsement_letter_versions v
                WHERE v.endorsement_request_id = r.id
                  AND v.version_number = COALESCE(r.letter_version, 1));
