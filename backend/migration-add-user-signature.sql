-- Migration: Electronic signature on the user account
-- ---------------------------------------------------------------------------
-- Adds the electronic signature reference to users (Manage Profile ->
-- Electronic Signature). The image itself is stored as a file under
-- uploads/signatures/ - the existing upload convention used by proposal
-- attachments and quarterly report documents - so the account row keeps only the
-- original file name and the stored path. No image data is written to a text
-- column.
--
-- Safe to run on existing databases: the change is additive and nullable, so
-- every account keeps working and simply has no signature until one is saved
-- (the API answers 404 in that case).
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS signature_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signature_file_path VARCHAR(500);

-- Lookup by the stored file is never done, so no index is added: the signature
-- is always addressed through the authenticated user's own account id.
