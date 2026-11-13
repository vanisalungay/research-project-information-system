# RPIS load and stress testing

Everything needed to reproduce the 500-concurrent-user measurements. The measured
results themselves are in [RESULTS.md](RESULTS.md).

## Safety first

These tests write data. They are only ever allowed to write to the **isolated test
database** (`rpis_test`), and three independent guards enforce that:

1. `application-loadtest.properties` — the only profile whose datasource points at
   `rpis_test`. It runs on its own port (8082) so a developer instance on 8081 is left
   alone.
2. `SyntheticTestDataSeeder` — calls `DatabaseSafetyGuard.requireTestDatabase(...)` as
   its **first** statement and aborts before writing anything if the live connection
   is not `rpis_test`.
3. `run-load-test.ps1` — asks the running backend `/api/health/ready` which database it
   is actually connected to and refuses to continue if it is not the expected one.

`mvn test` also targets `rpis_test`. Nothing in this directory can reach `rpis_db`.

## One-time setup

```powershell
# k6 (portable, no admin required)
Invoke-WebRequest -Uri 'https://github.com/grafana/k6/releases/download/v0.54.0/k6-v0.54.0-windows-amd64.zip' -OutFile 'tools/k6.zip'
Expand-Archive -Path 'tools/k6.zip' -DestinationPath 'tools' -Force
Remove-Item 'tools/k6.zip'
# -> tools/k6-v0.54.0-windows-amd64/k6.exe
```

`tools/` and `load-tests/results/` are git-ignored.

## Running the whole thing

```powershell
# 1. Start the backend on the load-test profile. The first start also generates the
#    synthetic data set (500 accounts, 1200 proposals, ~3.5 years of dates).
cd backend
$env:JAVA_HOME = "$PWD\jdk-17.0.11+9"
.\apache-maven-3.9.6\bin\mvn.cmd -o clean package -DskipTests
.\jdk-17.0.11+9\bin\java.exe -jar target\backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=loadtest

# 2. In another shell: run the 500-user test and the login burst, with resource sampling.
.\load-tests\run-load-test.ps1 -Scenario both
```

`run-load-test.ps1` verifies the target database, starts the sampler, runs k6, stops
the sampler and lists the artefacts. It exits non-zero if any k6 threshold is breached.

## Running the pieces by hand

```powershell
# the 500-concurrent-user scenario
k6 run -e BASE_URL=http://localhost:8082 load-tests/k6/rpis-500u.js

# 500 simultaneous logins
k6 run -e BASE_URL=http://localhost:8082 load-tests/k6/rpis-login-burst.js

# server-side resources for 7 minutes, every 5 s
.\load-tests\monitor\sample-resources.ps1 -BackendProcessId <pid> -DurationSeconds 420 -IntervalSeconds 5
```

## Scenario knobs

Both scripts are configured with `-e KEY=VALUE`:

| Variable | Default | Meaning |
|---|---|---|
| `BASE_URL` | `http://localhost:8082` | backend under test |
| `BROWSE_VUS` | `475` | read-only virtual users |
| `SUBMIT_VUS` | `25` | proposal-creating virtual users (`BROWSE_VUS + SUBMIT_VUS` is the concurrency being measured) |
| `PROPONENT_COUNT` | `500` | how many synthetic accounts the VUs are spread over |
| `MAX_SUBMISSIONS_PER_VU` | `20` | caps how many proposals a run can create |
| `RAMP` / `HOLD` / `DOWN` | `30s` / `4m` / `30s` | stage durations |
| `THINK_TIME` | `1.5` | seconds a virtual user "thinks" between screens |
| `USERS` | `500` | login-burst virtual users |
| `RPIS_LOADTEST_PASSWORD` | `LoadTest#2026` | password of the synthetic accounts |

Example — a quick smoke run:

```powershell
k6 run -e BROWSE_VUS=5 -e SUBMIT_VUS=2 -e RAMP=5s -e HOLD=10s -e DOWN=5s -e THINK_TIME=0.2 `
  load-tests/k6/rpis-500u.js
```

## Regenerating the synthetic data set

The generator is idempotent: it records the batch in `synthetic_seed_registry` and
skips if that batch is already present **and its rows are still there**. To force a
rebuild:

```sql
DELETE FROM synthetic_seed_registry WHERE batch_key = 'rpis-loadtest-v1';
```

then restart the backend. If the schema was recreated (for example by `mvn test`) the
generator notices that the marker has no rows behind it and rebuilds on its own.

To change the volume or the spread, edit the constants at the top of
`backend/src/main/java/com/rpis/backend/dev/SyntheticTestDataSeeder.java`
(`PROPONENT_COUNT`, `PROPOSAL_COUNT`, `ANNOUNCEMENT_COUNT`, `STATUS_WEIGHTS`, …).

## Rebuilding the database from scratch

`rpis_test` is disposable:

```powershell
$env:PGPASSWORD='1234'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d postgres `
    -c 'DROP DATABASE IF EXISTS rpis_test;' -c 'CREATE DATABASE rpis_test;'
```

Then start the backend with the `loadtest` profile and the data set is regenerated.

## Artefacts written to `load-tests/results/`

| File | Contents |
|---|---|
| `k6-500u-summary.txt` / `.json` | the 500-user run summary (p50/p95/p99 per flow, throughput, error rate) |
| `k6-login-burst-summary.txt` / `.json` | the login-burst summary |
| `k6-*-run.log` | full k6 console output, including the threshold report |
| `resources.csv` | backend CPU/RSS/threads and PostgreSQL connection counts, one row per interval |
| `mvn-test-final.log` | the backend test-suite run |
| `run*-*-summary.txt`, `run*-*-resources.csv` | the earlier runs quoted in RESULTS.md, kept so the before/after comparison is auditable |

## Reading `resources.csv`

| Column | Meaning |
|---|---|
| `jvm_cpu_seconds` | cumulative CPU seconds used by the backend JVM |
| `jvm_working_set_mb` | resident set size |
| `jvm_threads` / `jvm_handles` | thread and handle counts |
| `pg_total` | connections to the database (compare against the Hikari pool size) |
| `pg_active` | connections actually executing a query |
| `pg_idle` | connections checked out but not executing anything |
| `pg_idle_in_transaction` | connections holding an open transaction with no work |
| `pg_waiting_on_lock` | connections blocked on a lock |
| `pg_longest_query_s` | longest running query |

The key signal: if `pg_total` sits at the pool maximum while `pg_active` is near zero,
the requests are queueing for a *connection*, not for the database — which is exactly
the bottleneck this campaign found and fixed.

## A note on the host

Two runs in this campaign were destroyed by the host freezing (HikariCP reported
`Thread starvation or clock leap detected`). Before trusting a run, check the backend
log for that warning:

```powershell
Select-String -Path <backend-log> -Pattern 'Thread starvation|clock leap'
```

A run with that warning is not a valid measurement. See RESULTS.md §7.

