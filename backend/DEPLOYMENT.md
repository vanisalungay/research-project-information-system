# RPIS Deployment Runbook

How to deploy the Research Project Information System (RPIS) safely: several identical
backend instances behind a load balancer, one PostgreSQL server, and an upload directory
shared by every instance.

The guiding rule is that **the schema is changed deliberately, before the instances are
started, never by the application**. An instance started in the `prod` profile runs with
`spring.jpa.hibernate.ddl-auto=validate`, so it checks the schema and refuses to start
if it does not match - it never alters it.

---

## 1. What a deployment consists of

| Component | Notes |
| --- | --- |
| PostgreSQL | One server. All instances connect to the same database. |
| Backend instances | Two or more identical JVMs, `SPRING_PROFILES_ACTIVE=prod`. |
| Load balancer | nginx, HAProxy, an ingress controller or a cloud LB. |
| Upload directory | **One directory shared by every instance** (NFS, a shared volume or a mounted network path). |
| Frontend | Static build served by the web server or a CDN. |

### Why the upload directory must be shared

Uploaded documents are stored on the filesystem, not in the database. If each instance
had its own directory, a file uploaded through instance A would be missing when the load
balancer sent the next request to instance B - the user would see a document that
sometimes downloads and sometimes 404s. Every instance must therefore point at the same
absolute path:

```
RPIS_UPLOAD_DIR=/var/lib/rpis/uploads          # Linux
RPIS_UPLOAD_DIR=C:\rpis-data\uploads           # Windows
```

The value is absolute on purpose. It used to be the relative string `uploads`, which
resolved against the process working directory - which is why this repository contains
two divergent upload directories (`uploads/` and `backend/uploads/`).

---

## 2. Required environment variables

The `prod` profile references these **with no fallback**, so the application refuses to
start rather than run on a known value. The development defaults live in the repository,
which means anyone who can read the repository knows them.

| Variable | Required | Purpose |
| --- | --- | --- |
| `RPIS_DB_URL` | yes | `jdbc:postgresql://<host>:5432/rpis_db` |
| `RPIS_DB_USERNAME` | yes | Database account. Grant only the privileges the application needs. |
| `RPIS_DB_PASSWORD` | yes | Password for that account. |
| `RPIS_TOKEN_SECRET` | yes | Bearer-token signing key. **Must be identical on every instance.** |
| `RPIS_UPLOAD_DIR` | yes | Absolute path to the shared upload directory. |
| `RPIS_SERVER_PORT` | no | Per-instance port (default 8081). |
| `RPIS_PASSWORD_MIGRATION` | no | `true` runs the plain-text password migration at start-up. Default `false`. |
| `RPIS_CORS_ALLOWED_ORIGINS` | yes in practice | Comma-separated origins allowed to call the API. |
| `SPRING_PROFILES_ACTIVE` | yes | `prod`. |

### `RPIS_TOKEN_SECRET` must match across instances

A token is signed by whichever instance handled the sign-in and verified by whichever
instance receives the next request. Two instances with different secrets would reject
each other's tokens, and users would be signed out at random as the load balancer
switched between them. Generate it once and give every instance the same value.

Rotating it invalidates every issued token, so all users sign in again. Plan it.

```bash
openssl rand -base64 48
```

---

## 3. Pre-flight

Run these before touching the deployment. Do not skip the backup.

```bash
# 1. Back up the database. This is the only step that cannot be undone.
pg_dump -h <host> -U <user> -Fc rpis_db > rpis_db-$(date +%Y%m%d-%H%M%S).dump

# 2. Back up the upload directory, with a manifest so the copy can be proven intact.
cp -a "$RPIS_UPLOAD_DIR" "$RPIS_UPLOAD_DIR-backup-$(date +%Y%m%d-%H%M%S)"

# 3. Record the schema the migrations are about to change.
psql -h <host> -U <user> -d rpis_db -c "\d+ users" > schema-before.txt

# 4. Confirm no instance is running, or drain it at the load balancer.
```

On Windows, `tools/backup-uploads.ps1` performs step 2 and writes a SHA256 manifest.

---

## 4. Applying the schema migrations

The scripts are in `backend/`, named `migration-*.sql`, and are **idempotent**: each uses
`ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS` and `CREATE TABLE IF NOT EXISTS`,
so re-running one is harmless and a partially applied batch can simply be re-run.

### Ordered application

Apply them in the order below. The order matters only where a later script references
something an earlier one created (the announcement scripts, the endorsement workflow and
the proposal-code sequence).

```bash
cd backend
export PGHOST=<host> PGUSER=<user> PGDATABASE=rpis_db
# -v ON_ERROR_STOP=1 makes psql stop at the first error instead of continuing.
for f in \
  migration-add-announcement-details.sql \
  migration-add-announcement-funding-program.sql \
  migration-add-budget-file-path.sql \
  migration-add-cooperating-agencies.sql \
  migration-add-endorsement-workflow.sql \
  migration-add-endorsement-titles.sql \
  migration-add-field-level-remarks.sql \
  migration-add-notification-announcement-link.sql \
  migration-add-notification-fields.sql \
  migration-add-notification-preferences.sql \
  migration-add-performance-indexes.sql \
  migration-add-priority-agendas.sql \
  migration-add-proposal-address-contact.sql \
  migration-add-proposal-announcement-link.sql \
  migration-add-proposal-announcements.sql \
  migration-add-proposal-announcement-updates.sql \
  migration-add-proposal-code-sequence.sql \
  migration-add-proposal-id-fields.sql \
  migration-add-proposal-section-remarks.sql \
  migration-add-proposal-versions.sql \
  migration-add-proposal-version-unique.sql \
  migration-add-quarterly-progress-reports.sql \
  migration-add-quarterly-report-copied-from.sql \
  migration-add-quarterly-report-documents.sql \
  migration-add-references-json.sql \
  migration-add-revision-request-fields.sql \
  migration-add-technology-map-path.sql \
  migration-add-terminal-report-content.sql \
  migration-add-user-signature.sql \
  migration-add-workplan-json.sql \
  migration-rename-application-cycles-to-proposal-announcements.sql \
  migration-rename-dean-role.sql
do
  echo "applying $f"
  psql -v ON_ERROR_STOP=1 -f "$f"
done
```

`migration-add-performance-indexes.sql` is the slowest step, because it builds indexes.
On a large database, build them `CONCURRENTLY` outside a transaction to avoid blocking
writes; the script's plain `CREATE INDEX IF NOT EXISTS` is fine for the current data
volume.

### Note on `migration-add-budget-file-path.sql`

The file name says *budget*, but the column it adds is `beneficiaries_file_path` - the
Create Proposal form's section 25 field was renamed to "Line-Item Budget" while the
database column kept the older name. The mismatch is a naming inconsistency, not a bug:
the script and the entity agree on `beneficiaries_file_path`. Renaming the file would
break anyone's saved list of scripts, so it is left as it is.

### Note on `migration-rename-dean-role.sql`

The Dean / Department Chair is an **independent role**, not part of RPS, so its value is
`DEAN` - never `RPS_DEAN`. That value is not a display label: it is stored verbatim in
`users.role`, embedded in the bearer token and compared by `SecurityConfig`. A database
that still holds `RPS_DEAN` therefore cannot authenticate a Dean (the login endpoint
compares the submitted role with the stored one) and cannot authorize one either (the
token filter grants `ROLE_RPS_DEAN`, which no longer satisfies `hasRole(DEAN)`, so the
endorsement decisions answer 403).

**Run this migration together with the build that introduced it.** There is no
transitional dual-accept: the frontend, the backend and the database must all speak
`DEAN`. The script is idempotent and non-destructive (a plain data update - no
constraint, enum or trigger exists on the role columns), it also renames the role in the
audit columns that are displayed to users, and it ends with the verification query whose
counts must all be 0. A database provisioned from scratch by the current code needs
nothing: new Dean accounts are created as `DEAN`.

---

## 5. Verifying the schema before starting instances

The application validates the schema at start-up, so the cheapest verification is to
start one instance against a **clone** of the production database and confirm it comes
up. This never writes to production.

```bash
# Clone structure and data. Requires no active connections to rpis_db.
psql -d postgres -c "DROP DATABASE IF EXISTS rpis_migration_check;"
psql -d postgres -c "CREATE DATABASE rpis_migration_check TEMPLATE rpis_db;"

# Apply the migrations to the clone, then start one instance against it.
# RPIS_DB_URL=jdbc:postgresql://<host>:5432/rpis_migration_check
# SPRING_PROFILES_ACTIVE=prod

# A successful start ends with:
#   Started RpisBackendApplication in NN seconds
# A schema mismatch instead fails fast with:
#   Schema-validation: missing column [x] in table [y]

# Then drop the clone.
psql -d postgres -c "DROP DATABASE rpis_migration_check;"
```

This was verified against the real schema: the clone gained exactly
`users.notify_proposals` and `users.notify_deadlines`, and the application started with
`validate` without altering anything.

### Verification query

After applying the migrations to production, confirm the columns the application expects
are present:

```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'users'
  AND column_name IN ('notify_proposals', 'notify_deadlines')
ORDER BY column_name;
```

Both rows must be present. The readiness endpoint reports the same thing at runtime:

```bash
curl -s http://localhost:8081/api/health/ready
# {"status":"UP","database":"rpis_db", ...}
```

---

## 6. Starting the instances

```bash
export SPRING_PROFILES_ACTIVE=prod
export RPIS_DB_URL=jdbc:postgresql://<host>:5432/rpis_db
export RPIS_DB_USERNAME=rpis_app
export RPIS_DB_PASSWORD='<from the secret store>'
export RPIS_TOKEN_SECRET='<the shared signing key>'
export RPIS_UPLOAD_DIR=/var/lib/rpis/uploads
export RPIS_CORS_ALLOWED_ORIGINS=https://rpis.msunaawan.edu.ph
export RPIS_SERVER_PORT=8081

java -jar backend-0.0.1-SNAPSHOT.jar
```

Start one instance first and confirm it is healthy before starting the rest, so a bad
deployment is caught with no user impact.

### Connection pool sizing

The pool is sized against PostgreSQL's `max_connections`, which is a **server-wide**
limit, not a per-instance one:

```
instances x pool size  <=  max_connections - reserved
```

With the default `max_connections = 100` and ~10 connections reserved for administration,
**two instances must use a pool of at most ~45 each**. The shared configuration uses 40;
behind a load balancer with two instances, set it to 30 so `2 x 30 = 60` leaves ample
headroom. Exceeding the server limit does not degrade gracefully - connections are
refused outright, which is exactly the failure that was measured and fixed during the
load-testing work.

### Starting two instances against a schema that does not exist yet

`ddl-auto` is `none` in `prod`, so this does not arise in a real deployment: the schema is
applied by hand first (section 4). It does arise when two instances are started together
against an empty database with `ddl-auto=update`, which is what the load-test profile does.

Hibernate's schema update is not concurrency-safe. Both instances inspect the schema, both
conclude a table is missing, and both issue `CREATE TABLE`; the loser logs
`relation "..." already exists` and carries on, because Hibernate treats a failed DDL
statement as a warning rather than a fatal error. The observed messages were:

```
GenerationTarget encountered exception accepting command : Error executing DDL
  "create table ovcaf_validations (...)" via JDBC [ERROR: relation "ovcaf_validations" already exists]
```

Nothing was lost and both instances came up, but the start-up log is noisy and the outcome
depends on Hibernate tolerating the failure. **Apply the schema before starting any
instance** (section 5) and keep `ddl-auto=none` in production.

The data-writing start-up steps do *not* have this tolerance, which is why they take
explicit locks instead - see section 9 for the password migration, and note that the
default priority agenda framework is guarded the same way.

---

## 7. Health, readiness and graceful shutdown

| Endpoint | Meaning |
| --- | --- |
| `GET /api/health` | The process is alive. Use for a liveness probe. |
| `GET /api/health/ready` | The process can serve traffic: the database answers. Use for a readiness probe and for the load balancer's health check. |

Point the load balancer's health check at `/api/health/ready`, so an instance that has
lost the database is removed from rotation instead of returning errors. An instance
starting up is also not ready, which keeps traffic away from it until it can serve.

On shutdown the instance stops accepting new requests and waits for in-flight ones. When
stopping an instance - for a deploy or a restart - remove it at the load balancer first,
wait for in-flight requests to finish, then stop it.

---

## 8. Rolling back

The migrations are additive (`ADD COLUMN IF NOT EXISTS`, `CREATE TABLE IF NOT EXISTS`,
`CREATE INDEX IF NOT EXISTS`), so a rollback is a **code** rollback, not a schema one.
The previous build simply ignores columns it does not know about.

1. Remove one instance at a time from the load balancer and stop it.
2. Deploy the previous build.
3. Start it, confirm `/api/health/ready` is `UP`, then return it to rotation.
4. Repeat for the remaining instances.

Restore from the dump only if data was corrupted, and only as a last resort - it
discards everything written since the backup was taken.

---

## 9. Passwords: the plain-text migration

Accounts created before BCrypt was introduced store the password in plain text. There
are two independent ways they are upgraded, and **either one alone is enough**:

1. **Lazily, on sign-in.** When an account with a plain-text password signs in
   successfully, the password is re-hashed and stored. This needs no operator action and
   cannot lock anyone out, because it only ever runs after a correct password.
2. **In bulk, at start-up.** With `RPIS_PASSWORD_MIGRATION=true`, the application
   converts every remaining plain-text password before serving traffic, in batches, and
   logs the count.

### Only one instance runs the bulk pass

The bulk pass takes a PostgreSQL advisory lock before it starts, so two instances
starting at the same moment cannot both convert the table. The lock is taken with
`pg_try_advisory_lock` (namespace `"RPIS"`, purpose 1), which never blocks:

* the instance that gets it does the work and releases it when finished;
* every other instance logs that it is not running the migration and carries on serving
  traffic.

If the lock cannot be requested at all - the database is unreachable, say - that instance
runs **no** migration. Running it without proof of exclusivity is exactly what the lock
exists to prevent, and the lazy path still upgrades every account.

**Operationally: enable `RPIS_PASSWORD_MIGRATION` on one instance only, for one
start-up.**

### Before the migration: a backup that has been verified

The migration is not destructive and cannot lock anyone out, but it is a bulk write to
`users`, so it is treated like any other production write.

```bash
# 1. Dump the database.
pg_dump -h <host> -U postgres -Fc -f rpis_before_password_migration.dump rpis_db

# 2. VERIFY that the dump restores. An unverified backup is not a backup.
createdb rpis_restore_check
pg_restore -h <host> -U postgres -d rpis_restore_check rpis_before_password_migration.dump
psql -h <host> -U postgres -d rpis_restore_check -c 'SELECT count(*) FROM users;'
#    ^ must equal the users count read from rpis_db before the migration
psql -h <host> -U postgres -d rpis_restore_check -c "SELECT CASE WHEN password ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' THEN 'bcrypt' ELSE 'PLAIN TEXT' END AS shape, count(*) FROM users GROUP BY 1;"
dropdb rpis_restore_check
```

**Rollback.** The migration only rewrites the `password` column of accounts whose
password was readable text. Two options, in order of preference:

1. **Do nothing.** Both stored shapes verify, so the application is never in a state where
   nobody can sign in, and a partial run leaves the unconverted rows readable by the lazy
   path. This is the normal case.
2. **Restore the dump.** Only if data was corrupted, and only as a last resort - it
   discards every write made since the dump was taken.

### Running it once

```bash
# On ONE instance, for ONE start-up only.
export RPIS_PASSWORD_MIGRATION=true
java -jar backend-0.0.1-SNAPSHOT.jar
```

The start-up log reports the outcome and nothing else - **a count, never a password, a
hash or an e-mail address**:

```
Password migration: converted 24 account(s) to BCrypt.
```

or, on a subsequent run:

```
Password migration: no plain-text passwords found.
```

### Verify, then disable

```sql
SELECT CASE
         WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' OR password LIKE '$2y$%'
           THEN 'bcrypt'
         ELSE 'PLAIN TEXT'
       END AS shape,
       count(*)
FROM users
GROUP BY 1;
```

`PLAIN TEXT` must be `0`. Then **unset `RPIS_PASSWORD_MIGRATION` and restart**, so a
future start-up does not scan the table for nothing.

### Notes

* The migration is **idempotent**: a row is converted only when it is not already a hash,
  so re-running converts nothing and never double-hashes.
* It **degrades rather than blocks**: if it fails, the application still starts and the
  lazy path covers every account.
* The operator-facing count is a single `COUNT(*)` using the same regular expression the
  migration uses, so asking "how many are left?" never loads the user table into memory.
---

## 10. Uploaded documents: where they live, and what is in Git history

Uploaded documents are runtime user data. They are no longer tracked by Git
(`uploads/` and `backend/uploads/` are in `.gitignore`), and the live copies live in
`RPIS_UPLOAD_DIR`.

**Ignoring a directory does not remove what is already in history.** The earlier commits
still contain the signatures and endorsement letters that were committed. Removing them
from history rewrites every commit hash and invalidates every clone and fork, so it is
**not** done here. If the project decides it is necessary, the options are:

1. **Leave it.** Accept that the historical files remain, and treat them as
   already-disclosed. Reasonable if they are not sensitive.
2. **Rewrite with `git filter-repo`.** Removes the paths from every commit. Requires
   coordination: everyone re-clones, and every open pull request must be re-created.
3. **Rewrite and rotate.** As above, then rotate anything the exposed files could
   compromise.

Decide with the data owner before doing anything; option 2 is a disruptive, one-way
operation.

---

## 11. Deployment checklist

- [ ] Database backed up (`pg_dump`).
- [ ] Upload directory backed up, with a manifest.
- [ ] Migrations applied in order, `ON_ERROR_STOP=1`, no errors.
- [ ] Verification query returns both `notify_*` columns.
- [ ] One instance started against a clone with `validate`; it came up.
- [ ] `RPIS_TOKEN_SECRET` identical on every instance, from the secret store (not a file in the repository).
- [ ] `RPIS_UPLOAD_DIR` absolute and shared by every instance.
- [ ] Pool size x instance count within `max_connections`.
- [ ] Health check pointed at `/api/health/ready`.
- [ ] `RPIS_CORS_ALLOWED_ORIGINS` lists the real front-end origin, not `*`.
- [ ] First instance healthy before the rest are started.
- [ ] Password shape query returns `0` for `PLAIN TEXT`.

