# Research Project Information System (RPIS)

RPIS is a web application for the research office of **Mindanao State University at
Naawan**. It organizes and automates the submission, endorsement, review, approval,
funding validation and monitoring of research project proposals.

Proponents submit proposals online, upload supporting documents and track their
status; the concerned offices review and act on them in sequence; approved projects
are monitored through quarterly and terminal reports. For approved proposals, the
system also records requests for additional funds (such as travel requests), which
the Office of the Vice Chancellor for Academic Affairs (OVCAF) checks for validity and
completeness. RPIS only documents and tracks these requests — it does not release
funds.

The application is a single-page Vue frontend backed by a Spring Boot REST API and a
PostgreSQL database.

## Contents

- [Roles](#roles)
- [Proposal lifecycle](#proposal-lifecycle)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Testing](#testing)
- [Documentation](#documentation)
- [Project status](#project-status)

## Roles

| Role | UI label | Scope |
| --- | --- | --- |
| `PROPONENT` | Proponent | Own proposals, endorsements, quarterly and terminal reports |
| `DEAN` | Dean | Endorsement requests for their own college only |
| `RPS_STAFF` | ORPS | Proposal review and endorsement, revision forwarding, monitoring |
| `RPS_ADMIN` | — | Accounts, Deans, announcements, priority agendas, activity log |
| `OVCRIGE` | OVCRIGE | University-wide review; forwards proposals to REC or OC |
| `REC` | REC | Research Ethics Committee evaluation |
| `OC` | Chancellor (OC) | Final approval, budget classification and Special Order issuance |
| `OVCAF` | OVCAF | Financial validation and recording of fund releases |

Access is enforced by the backend; the frontend hides navigation and pages a role
cannot use, but the API is the security boundary.

## Proposal lifecycle

```text
Proponent DRAFT -> FOR REVIEW
  -> RPS Staff: ENDORSED (to OVCRIGE) | REJECTED | RPS_RETURNED (back to Proponent)
OVCRIGE ENDORSED -> UNDER_REVIEW (forwarded to REC)
REC UNDER_REVIEW -> REC_APPROVED | RETURNED_TO_RPS | REJECTED
OVCRIGE REC_APPROVED -> FOR_OC_APPROVAL
OC FOR_OC_APPROVAL -> FOR_OVCAF_APPROVAL (with budget) | APPROVED (without budget)
OVCAF FOR_OVCAF_APPROVAL -> READY_FOR_RELEASE -> APPROVED (fund release recorded)
APPROVED / RELEASED + issued Special Order -> Q1 -> Q2 -> Q3 -> TERMINAL -> COMPLETED
```

`APPROVED` means implementation is approved; `COMPLETED` means reporting has
finished. They are distinct states.

## Features

**Proponents**
- Self-registration (subject to administrator approval), profile, notification
  preferences and electronic signature.
- Four-step submission wizard: call for proposals, 27-section proposal details,
  certification and endorsement, then review and submit.
- Attachments for the technology map, line-item budget, endorsement document and
  signed certification.
- Dean endorsement requests, revisions with section- and field-level remarks, and
  proposal monitoring.
- Quarterly progress reports (Q1–Q3) and a 14-section terminal report.

**Review and approval offices**
- Role-specific dashboards and queues.
- RPS Staff review, endorsement, return and rejection, plus revision forwarding.
- Dean endorsement review with server-generated PDF endorsement letters.
- OVCRIGE forwarding to REC/OC, return for revision, rejection and field remarks.
- REC weighted evaluation form (15 scored criteria) with approval, rejection and
  return-for-revision actions.
- Chancellor final approval, with/without-budget classification, Special Order
  number and document, and draft saving.
- OVCAF inbox, line-item budget validation with a financial checklist, fund-release
  recording, and financial reports.

**Cross-cutting**
- Proposal announcements (calls for proposals) and priority agenda frameworks.
- In-app notifications with deep links, plus e-mail support (SMTP configurable).
- Activity logging for administrators.
- Server-side PDF generation for endorsement letters, proposal documents and
  terminal reports.
- Philippine address cascade with server-side address validation.
- Stateless bearer-token authentication; signing out invalidates issued tokens
  server-side.

## Tech stack

| Layer | Technologies |
| --- | --- |
| Frontend | Vue 3.5 (Composition API, `<script setup>`), Vite 7, Pinia 3, Vue Router 4, Axios, JavaScript, Prettier |
| Backend | Java 17, Spring Boot 3.2.5 (Web, Data JPA, Security, Actuator, Mail), Hibernate, Maven |
| Database | PostgreSQL, HikariCP connection pool |
| Documents | OpenPDF (endorsement letters), openhtmltopdf + jsoup (proposal and report PDFs), mammoth (DOCX preview) |
| Auth | HMAC-SHA256 signed bearer tokens, BCrypt password hashing |

## Repository layout

```text
.
├── src/                    Vue single-page application
│   ├── views/              Pages grouped by role (Proponent, Dean, Ovc, Rec, Oc, Ovcaf, Rps, Admin, ...)
│   ├── components/         Shared UI and document components
│   ├── layouts/            DefaultLayout (sidebar) and NoNavLayout (public pages)
│   ├── router/             Route table and authentication guards
│   ├── stores/             Pinia stores (session, notifications, Dean attention)
│   ├── utils/              API client, form logic, document builders, Node tests
│   └── assets/             Shared CSS and images
├── public/                 Static assets: Philippine location data, document templates
├── backend/                Spring Boot REST API
│   ├── src/main/java/com/rpis/backend/
│   │   ├── controller/     REST endpoints
│   │   ├── service/        Business logic
│   │   ├── model/          JPA entities
│   │   ├── repository/     Spring Data repositories
│   │   ├── config/         Security, token filter, startup guards
│   │   ├── storage/        Upload storage abstraction (local filesystem)
│   │   └── dto/ security/ util/ dev/
│   ├── src/main/resources/ application*.properties, address catalog, PDF templates
│   ├── src/test/           Backend test suite
│   ├── migration-*.sql     Ordered, additive schema migrations
│   ├── schema.sql          Destructive reset for *_dev / *_test databases only
│   └── DEPLOYMENT.md       Production deployment runbook
├── docs/                   Functionality evaluation plan and database ERD
├── load-tests/             k6 load/stress scenarios, runners and results
├── scripts/                Philippine location catalog generators
├── flow/project scope.txt  Original project scope
├── .env.example            Frontend environment template
└── run-backend.bat         Windows local launcher (applies migrations, starts backend)
```

## Prerequisites

- **Node.js** `^20.19.0 || >=22.12.0` (see `package.json`)
- **JDK 17** — the backend targets Java 17
- **Maven 3.9+** — no Maven wrapper is included
- **PostgreSQL** running locally (default port `5432`)
- **npm**

## Getting started

### 1. Clone the repository

```bash
git clone https://github.com/vanisalungay/research-project-information-system.git
cd research-project-information-system
```

### 2. Start the frontend

```bash
npm install
cp .env.example .env.local
npm run dev
```

Vite serves the app at <http://localhost:5173>. `.env.local` is optional — the
defaults already point at `http://localhost:8081` with a 30-second timeout — but copy
it if you need to change the API origin.

### 3. Create and migrate the database

The backend does not create or alter the schema on its own (`ddl-auto=none`); the
schema is owned by the SQL migrations in `backend/`. Apply them before starting the
backend.

For a **fresh local database**, use a name ending in `_dev` so the reset script's
safety guard accepts it:

```bash
createdb rpis_dev
psql -d rpis_dev -f backend/schema.sql
```

`backend/schema.sql` is a destructive development reset and deliberately refuses to
run against a database whose name does not end in `_dev` or `_test`. Never run it
against a database with real data.

Then apply the additive migrations in order, from the `backend/` directory:

```bash
cd backend
export PGHOST=127.0.0.1 PGUSER=postgres PGDATABASE=rpis_dev
```

<details>
<summary>Migration order (61 scripts)</summary>

```bash
for f in \
  migration-rename-application-cycles-to-proposal-announcements.sql \
  migration-add-proposal-announcements.sql \
  migration-add-announcement-details.sql \
  migration-add-announcement-funding-program.sql \
  migration-add-proposal-announcement-updates.sql \
  migration-add-notification-announcement-link.sql \
  migration-add-budget-file-path.sql \
  migration-add-cooperating-agencies.sql \
  migration-add-endorsement-workflow.sql \
  migration-add-endorsement-titles.sql \
  migration-add-proposal-section-remarks.sql \
  migration-add-field-level-remarks.sql \
  migration-add-notification-fields.sql \
  migration-add-notification-preferences.sql \
  migration-add-notification-preferences-configured.sql \
  migration-add-notification-dedupe-key.sql \
  migration-add-revision-deadline-index.sql \
  migration-add-priority-agendas.sql \
  migration-add-proposal-address-contact.sql \
  migration-add-proposal-announcement-link.sql \
  migration-add-proposal-code-sequence.sql \
  migration-add-proposal-id-fields.sql \
  migration-add-proposal-versions.sql \
  migration-add-proposal-version-unique.sql \
  migration-add-quarterly-progress-reports.sql \
  migration-add-quarterly-report-copied-from.sql \
  migration-add-quarterly-report-documents.sql \
  migration-add-references-json.sql \
  migration-add-revision-request-fields.sql \
  migration-add-risk-management-json.sql \
  migration-add-proposal-endorsement-document.sql \
  migration-add-proposal-certification.sql \
  migration-add-technology-map-path.sql \
  migration-add-terminal-report-content.sql \
  migration-add-user-signature.sql \
  migration-add-workplan-json.sql \
  migration-rename-dean-role.sql \
  migration-add-must-change-password.sql \
  migration-add-token-version.sql \
  migration-add-user-roles.sql \
  migration-add-dean-assignments.sql \
  migration-fix-institutional-email-domain.sql \
  migration-add-gad-checklist.sql \
  migration-add-activity-logs.sql \
  migration-add-endorsement-document-code-unique.sql \
  migration-add-endorsement-letter-versions.sql \
  migration-add-proposal-needs-budget.sql \
  migration-add-performance-indexes.sql \
  migration-01-db-audit-integrity.sql \
  migration-02-db-audit-dedupe.sql \
  migration-03-db-audit-status-history.sql \
  migration-add-proposal-submitted-at.sql \
  migration-add-proposal-endorsed-at.sql \
  migration-norm-01-relational-integrity.sql \
  migration-norm-02-relational-observability.sql \
  migration-norm-03-fk-consolidation.sql \
  migration-norm-04-proposal-status-fks.sql \
  migration-widen-notification-type-check.sql \
  migration-add-notification-recipient-role.sql \
  migration-widen-activity-log-checks.sql \
  migration-add-proposal-return-kind.sql
do
  echo "applying $f"
  psql -v ON_ERROR_STOP=1 -f "$f"
done
```

</details>

The migrations are idempotent and additive, so re-running the list is harmless. For
an existing database, apply the same list and set `RPIS_DB_URL` to that database.
`migration-complete-finished-projects.sql` is a separate one-off data correction, not
part of the schema list.

`run-backend.bat` performs this same ordered migration run automatically on Windows
before starting the backend. Set `RPIS_SKIP_MIGRATIONS=1` to skip it.

### 4. Start the backend

From `backend/`:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev \
RPIS_DB_URL=jdbc:postgresql://127.0.0.1:5432/rpis_dev \
RPIS_DB_USER=postgres \
RPIS_DB_PASSWORD=1234 \
RPIS_DB_EXPECTED=rpis_dev \
mvn spring-boot:run
```

The API listens on <http://localhost:8081>. Adjust the URL, credentials, database
name and `RPIS_DB_EXPECTED` to match your PostgreSQL setup. `RPIS_DB_URL` is required
and has no default.

The `dev` profile enables SQL logging and seeds the demonstration accounts below on
first start. The application refuses to start if the connected database name does not
match `RPIS_DB_EXPECTED` (default `rpis_db`).

Confirm the backend is up:

```bash
curl http://localhost:8081/api/health
curl http://localhost:8081/api/health/ready
```

### 5. Open the application

With both servers running, visit <http://localhost:5173>.

#### Local demonstration accounts

The `dev` profile creates one approved account per role:

| Role | E-mail |
| --- | --- |
| Proponent | `proponent@msunaawan.edu.ph` |
| RPS Staff (ORPS) | `rpsstaff@msunaawan.edu.ph` |
| RPS Admin | `rpsadmin@msunaawan.edu.ph` |
| OVCRIGE | `ovcrige@msunaawan.edu.ph` |
| REC | `rec@msunaawan.edu.ph` |
| OVCAF | `ovcaf@msunaawan.edu.ph` |
| OC | `oc@msunaawan.edu.ph` |

Their passwords are defined in `UserService.seedUsers()` and are for local
development only. Never enable demo seeding on a shared or production database. The
login page currently presents role cards for Proponent, Dean, ORPS, OVCRIGE, REC,
OVCAF and Chancellor; RPS Admin sign-in is not exposed there.

### Production build

```bash
npm run build                 # static assets in dist/
cd backend && mvn clean package   # backend/target/backend-0.0.1-SNAPSHOT.jar
```

Serve `dist/` from a web server or CDN, point the frontend at the API origin
(`VITE_API_BASE_URL`) and run the JAR with the `prod` profile. See
[`backend/DEPLOYMENT.md`](backend/DEPLOYMENT.md) for the full runbook.

## Configuration

### Frontend

Vite exposes only variables prefixed with `VITE_`. Copy `.env.example` to
`.env.local` and adjust:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8081` | Origin of the backend (or the load balancer) |
| `VITE_API_TIMEOUT_MS` | `30000` | Request timeout in milliseconds |

There is no Vite proxy; the browser calls the backend origin directly.

### Backend

Configuration lives in `backend/src/main/resources/application*.properties`, with
environment overrides. The common ones:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | none | `dev`, `prod` or `loadtest` |
| `RPIS_DB_URL` | **required** | JDBC URL, e.g. `jdbc:postgresql://127.0.0.1:5432/rpis_db` |
| `RPIS_DB_USER` | `postgres` | Database user |
| `RPIS_DB_PASSWORD` | local default | Database password |
| `RPIS_DB_EXPECTED` | `rpis_db` | Startup guard: required live database name |
| `RPIS_SERVER_PORT` | `8081` | HTTP port |
| `RPIS_UPLOAD_DIR` | `uploads` | Root directory for uploaded documents |
| `RPIS_TOKEN_SECRET` | local default | Bearer-token signing key; must be identical on every instance |
| `RPIS_CORS_ALLOWED_ORIGINS` | `*` | Comma-separated allowed frontend origins |
| `RPIS_DB_POOL_SIZE` | `40` (`10` in `dev`) | HikariCP maximum pool size |
| `RPIS_JPA_DDL_AUTO` | `none` | Hibernate schema mode; `update` only for disposable databases |
| `RPIS_PASSWORD_MIGRATION` | `false` | Convert legacy plain-text passwords to BCrypt at startup |
| `RPIS_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | Gmail SMTP placeholders | Outgoing e-mail |

The `prod` profile requires `RPIS_DB_PASSWORD` and `RPIS_TOKEN_SECRET` with no
fallback, and validates — never alters — the schema. Each instance behind a load
balancer must share the same absolute `RPIS_UPLOAD_DIR`; otherwise a document
uploaded through one instance may 404 through another. See
[`backend/DEPLOYMENT.md`](backend/DEPLOYMENT.md) for the complete variable list,
migration ordering, connection-pool sizing and rollback guidance.

Uploaded documents are runtime data and are git-ignored (`uploads/`,
`backend/uploads/`). They should live in the configured storage root, not in the
repository.

## Testing

### Frontend

```bash
npm test            # Node's built-in test runner over src/**/*.test.js
```

The frontend tests are plain Node test-runner files, not Vitest or Jest. Prettier is
available via `npm run format`. `npm run type-check` is declared in `package.json`,
but no `tsconfig.json` is currently committed, so it cannot run as-is.

### Backend

The test suite requires a running PostgreSQL server with a disposable `rpis_test`
database. Tests use the `test` profile, create and drop the schema with
`ddl-auto=create-drop`, and write uploads under `backend/target/test-uploads`.

```bash
createdb rpis_test
cd backend
mvn test
```

Use JDK 17 for the suite, matching the project target. Run a targeted test class with
`mvn test -Dtest=ClassName`.

### Load tests

The k6 scenarios in `load-tests/` measure the backend at 500 concurrent users against
the isolated `rpis_test` database. See [`load-tests/README.md`](load-tests/README.md)
for setup and [`load-tests/RESULTS.md`](load-tests/RESULTS.md) for the measured
results.

## Documentation

| Document | Contents |
| --- | --- |
| [`backend/DEPLOYMENT.md`](backend/DEPLOYMENT.md) | Production deployment runbook: migrations, environment variables, health checks, rolling restarts, password migration and upload storage |
| [`docs/system-functionality-evaluation-plan.md`](docs/system-functionality-evaluation-plan.md) | Role-based functionality inventory, test scenarios and known gaps |
| [`docs/erd/README.md`](docs/erd/README.md) | Database ERD (interactive HTML, SVG, draw.io, Graphviz and JSON catalog snapshot) |
| [`load-tests/README.md`](load-tests/README.md) | k6 load and stress test setup and scenarios |
| [`load-tests/RESULTS.md`](load-tests/RESULTS.md) | Measured 500-user results and pool/thread sizing rationale |
| [`flow/project scope.txt`](flow/project%20scope.txt) | Original project scope statement |

## Project status

RPIS is an academic project under active development. The core proposal workflow —
submission, endorsement, review, approval, financial validation, fund-release
recording and project reporting — is implemented across the eight roles above. The
[`docs/system-functionality-evaluation-plan.md`](docs/system-functionality-evaluation-plan.md)
document records the current implementation status of each module, including areas
that are partial or unavailable, and is the authoritative reference when in doubt.
