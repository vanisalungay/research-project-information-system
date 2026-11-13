# Official endorsement letter template

`endorsement-letter-template.docx` is the **official MSU-Naawan endorsement letter** the
institution supplied (the current source template). It is the authoritative reference for the
letter RPIS issues; keep it here so it survives deployment and is versioned with the code.

## How it is used

The DOCX is **not** converted at runtime and is **not** used as a background image. Its layout
is reproduced faithfully in code by
`backend/src/main/java/com/rpis/backend/service/EndorsementLetterService.java` (OpenPDF /
iText, already the project's PDF engine), so every letter is a real, selectable-text PDF that
is generated, stored and served by the existing letter infrastructure.

The DOCX carries the whole letterhead as one anchored image. That image is **byte-identical**
(sha256) to `msun-letterhead-a4.png` in this directory, and the anchors in the DOCX
(header `-904240 / -274320` EMU, footer `-910590 / -379095` EMU, `7571105` EMU wide) are the
exact values the renderer already used, so the letterhead, seal, divider and footer are the
official artwork - never redrawn, stretched or substituted.

## Geometry

| Property | Official DOCX | Renderer |
|---|---|---|
| Page | `11906 x 16838` twips = **A4 portrait** | A4 portrait |
| Margins | `1440` twips = **1 in = 72 pt** on all four sides | 72 pt |
| Header / footer band | 720 twips from the edge | placed at the DOCX EMU anchors |
| Body | single spacing | 14 pt leading |

## Fonts

| Element | Official DOCX | Renderer | Note |
|---|---|---|---|
| Body ("Normal (Web)" style) | **Times New Roman 12 pt** | **Times-Roman / Times-Bold 12 pt** | standard PDF equivalent |
| "College of ..." line | **Monotype Corsiva 14 pt** | **Times 14 pt** | Corsiva is a licensed font embedded (obfuscated) in the DOCX and is not a standard PDF font - the one deliberate typographic difference |
| Document Code label / value | **Arial 6 pt bold italic, `#C00000`** | **Helvetica-BoldOblique 6 pt, `#C00000`** | Helvetica is the standard metric-compatible stand-in for Arial |

A licensed font file can be supplied with `rpis.endorsement.font-path` to replace Times.

## Placeholders and how RPIS fills them

| Official placeholder / line | RPIS source |
|---|---|
| `[College of _______________________]` | the endorsement's `college` (prefixed "College of" only when the value does not already name a unit) |
| `[Month Day, Year]` | the endorsement approval date (`endorsement_date`) |
| `[NAME OF THE DIRECTOR]` | `endorsement_letter_settings.director_name` + `director_surname`; falls back to "The Director" when unset |
| `Director` | `endorsement_letter_settings.director_designation` (default "Director") |
| `Office of Research and Publication Services (ORPS)` | `endorsement_letter_settings.office_name` |
| `[Name of the University]` | `endorsement_letter_settings.university_name` |
| `Subject: ...` | fixed official wording (kept verbatim) |
| `[Surname]` of the salutation | `endorsement_letter_settings.director_surname`; falls back to "Dear Director:" when unset |
| `[Title of the Research Proposal]` | the endorsement's `proposal_title` |
| `[Name of Faculty/Researcher]` | the Proponent's name |
| `[Name of Department/College]` | the endorsement's `department`, else its `college` |
| `[NAME OF THE DEAN]` | the Dean who approved (`decided_by_name`) |
| `[Name of the College]` | the endorsement's `college` |
| Dean signature | the signature snapshot stored at approval (`signature_file_path`) |
| Document Code (the white box in the DOCX) | the saved `letter_document_code`, printed as plain right-aligned text in the upper-right corner - **no box, border or background** (per the institutional instruction) |

## Fields RPIS cannot populate yet

- **`[NAME OF THE DIRECTOR]` and `[Surname]`**: the `director_name` / `director_surname`
  columns of `endorsement_letter_settings` are **NULL** in the current database (the ORPS
  Director was previously not part of the workflow). Until an administrator sets them, the
  letter falls back to "The Director" / "Dear Director:" instead of inventing a name. Set
  them (or the `rpis.endorsement.director-name` / `-surname` properties) to populate the
  official placeholders.

## Notes

- The DOCX's Document Code block is a page-anchored **white rectangle with a white border**
  (theme `lt1` = window) at `x=367.1 pt, y=135.35 pt, 216 x 111.75 pt`; it is invisible on
  the white page. The renderer prints the code as plain right-aligned text and draws no
  rectangle at all, which matches the requested appearance.
- The DOCX subject line contains the word "Proposal" twice
  ("... Project Proposal Proposal"); it is reproduced verbatim so the letter matches the
  official file exactly. Flagged for confirmation.
- The DOCX embeds the letterhead image and seven (partly obfuscated) fonts; only the image is
  needed, and only its already-shipped copy is used.
