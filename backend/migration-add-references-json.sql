-- Migration: APA reference builder for the Literature Cited section (section 23)
-- ---------------------------------------------------------------------------
-- The Literature Cited section used to be one free-text column (`references_text`)
-- that the Create Proposal form filled in by hand - and which the API never actually
-- stored, because ProposalRequest had no field for it. Both are fixed here:
--
--   references_text  the plain-text rendering of every reference, one reference per
--                    block separated by a blank line. This stays the value every
--                    existing screen reads (reviewer views, exports, the printable
--                    official form, submission history), so nothing has to change
--                    outside the proponent form.
--   references_json  the structured entries behind the list (a JSON array of
--                    {type, fields, text}), written by the APA reference builder. It
--                    is what lets the proponent reopen a reference for editing and
--                    keeps the italics of journal titles, book titles, volumes, ...
--                    after a save and a reload. It is NULL for every proposal that
--                    was saved before the builder existed (or written by hand), and
--                    the form then falls back to reading the plain text, so no
--                    existing record is lost.
--
-- Safe to run on existing databases: the change is additive and nullable.
ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS references_json TEXT;

-- No index: the entries are only ever read together with their proposal.
