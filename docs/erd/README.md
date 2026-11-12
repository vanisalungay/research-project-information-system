# RPIS Business ERD (presentation copy)

This folder contains a presentation ERD of the RPIS database. It is generated from
the live PostgreSQL catalog and saved **outside** the database.

- **30 business tables** in schema `public`
- **57 foreign-key relationships**
- **`public.database_migration_rollback_log` is intentionally excluded**

The rollback log is migration infrastructure used by the NORM-04 rollback script. It is
retained in PostgreSQL but is not a business entity, so it is not part of this ERD.
No database object was created, altered, moved, renamed, or dropped to produce these
files.

## Files

| File | Purpose |
| --- | --- |
| `rpis-business-erd.html` | **Open this to present.** Self-contained offline viewer with drag-to-pan, wheel zoom, FK-label toggle, table search, and print support. No network access required. |
| `rpis-business-erd.svg` | Standalone vector diagram for slides, documents, or a browser. Non-NORM FK names are hidden; hover a line for the constraint and its actions. |
| `rpis-business-erd.drawio` | Editable diagram for [diagrams.net](https://app.diagrams.net) or the VS Code *Draw.io Integration* extension. All 57 edges are connected to their table boxes. |
| `rpis-business-erd.dot` | Graphviz source. Render with `dot -Tsvg rpis-business-erd.dot -o graphviz.svg` if Graphviz is installed. |
| `rpis-business-erd.json` | Exact catalog snapshot: all tables, columns, types, nullability, defaults, primary keys, foreign keys, and referential actions. |
| `extract-erd-data.sql` | Read-only psql script that produces the JSON snapshot. |
| `generate-erd.mjs` | Dependency-free Node generator for the SVG, HTML, draw.io, and DOT files. |
| `verify-erd.mjs` | Structural checks for counts, endpoints, NORM-04 edges, and registry exclusion. |

## What the diagram shows

- Every column of every business table, with its PostgreSQL type and a `?` marker
  when the column is nullable.
- `PK` badges for primary-key columns and `FK` badges for foreign-key columns.
- One line per foreign key, with crow's-foot (`many`) at the child end and a single
  tick (`one`) at the parent end.
- The three NORM-04 relationships in red:
  - `fk_proposals_status`: `proposals.status` → `proposal_statuses.code`
  - `fk_psh_from_status`: `proposal_status_history.from_status` → `proposal_statuses.code`
  - `fk_psh_to_status`: `proposal_status_history.to_status` → `proposal_statuses.code`
- Tables grouped by area: reference/identity, proposals core, proposal detail,
  endorsement/certification, reports/OVCAF, and notifications/audit.

## Regenerating the snapshot

Run from the repository root. The script opens a `READ ONLY` transaction and rolls
it back; it performs no DDL or DML.

```powershell
psql -h localhost -U postgres -d rpis_db -v ON_ERROR_STOP=1 -f docs/erd/extract-erd-data.sql
node docs/erd/generate-erd.mjs
node docs/erd/verify-erd.mjs
```

`psql` will prompt for the database password. Do not put the password in the SQL
file or commit it. On Windows, if `psql` is not on `PATH`, run it from
`C:\Program Files\PostgreSQL\18\bin\psql.exe`.

## Editing

- **Manual layout or wording changes:** open `rpis-business-erd.drawio` in
  diagrams.net / the VS Code Draw.io Integration extension. This is the editable
  copy; changes made only there are not reflected in the SVG or HTML.
- **Regenerating with different grouping, widths, or labels:** edit the constants
  near the top of `generate-erd.mjs` (`COLUMNS`, `GROUPS`, `TABLE_GROUP`) and rerun
  the generator. Regeneration overwrites the SVG, HTML, drawio, and DOT files.
- **Data changes:** rerun `extract-erd-data.sql` first, then the generator. The
  generator refuses to run if the snapshot does not contain exactly 30 business
  tables and 57 foreign keys, or if the rollback registry is present.

## Verification

`verify-erd.mjs` checks:

- 30 business tables and 57 foreign keys in the snapshot.
- Every foreign-key endpoint exists in the 30-table set.
- The three NORM-04 constraints have the expected columns, `ON DELETE RESTRICT`,
  `ON UPDATE NO ACTION`, and `validated = true`.
- The SVG contains 30 table nodes, 57 edges, and exactly 3 NORM-04 edges.
- The draw.io file contains 30 table boxes and 57 connected edges.
- The DOT file contains 57 edges.
- `database_migration_rollback_log` is absent from the snapshot and the SVG.

## Snapshot metadata

- Schema: `public`
- Generated from: `rpis_db` on the date recorded in
  `rpis-business-erd.json` (`generated_at`)
- Presentation format: dependency-free SVG + offline HTML viewer
- Database changes made for this ERD: **none**
