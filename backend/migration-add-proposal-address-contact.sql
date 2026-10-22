-- Migration: Structured Address and Contact sections on a proposal
-- ---------------------------------------------------------------------------
-- The Create Proposal form used to capture one free-text "Address / Contact"
-- field. It is now two sections:
--
--   Address *  Country, Region, Province, District, Municipality / City and
--              Barangay - cascading dropdowns built from the bundled PSGC
--              dataset, with manual entry for any country other than the
--              Philippines.
--   Contact *  A contact type (Contact Number or Email Address) plus its value.
--
-- The combined `address` column is kept and is still written on every save,
-- derived from the six address parts plus the contact, so the proposal details
-- screen, reviewer views, exports and printouts - which all read that single
-- column - keep showing the complete "Address / Contact" text.
--
-- Nothing is migrated away from `address`: existing rows keep their current
-- value and the new columns stay NULL until the proponent edits the proposal,
-- so no previously saved address or contact information is discarded.
--
-- Safe to run on existing databases: the change is additive and nullable.
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS address_country VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_region VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_province VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_district VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_municipality VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_barangay VARCHAR(120),
    ADD COLUMN IF NOT EXISTS contact_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS contact_value VARCHAR(255);

-- No index: the parts are only ever read together with their proposal, never
-- searched by value.
