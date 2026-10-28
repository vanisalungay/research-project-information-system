# RPIS load-test results

Measured results for the 500-concurrent-user requirement, the bottlenecks the
measurements exposed, and what was changed in response.

**Everything in this file is measured on the machine described below. No figure is
projected, estimated or assumed.**

---

## 1. Test environment

| Item | Value |
|---|---|
| CPU | 8 logical processors |
| RAM | 23.7 GB |
| OS | Windows (x64) |
| Backend | Spring Boot 3.2.5 / Hibernate 6.4.4, JDK 17.0.11, one instance |
| Database | PostgreSQL 18, `max_connections = 100`, `shared_buffers = 128 MB` |
| Database under test | `rpis_test` (isolated; `rpis_db` is never written to) |
| Load generator | k6 v0.54.0, running on the same host as the server |
| Data set | 518 synthetic accounts, 1200 synthetic proposals (see §5) |

The generator sharing the host with the server makes these numbers **conservative**:
the two compete for the same 8 cores.

## 2. Scenarios

| Scenario | What it does |
|---|---|
| `k6/rpis-500u.js` | **500 peak VUs** = 475 `browse` + 25 `submit`; ramps 0 → 250 → 500 over 60 s, holds 500 for 4 min, ramps down 30 s. `browse` logs in once, then repeatedly opens the dashboard, pages the proposal list, opens a proposal, reads notifications and reads the calls for proposals. `submit` logs in once and creates proposals. |
| `k6/rpis-login-burst.js` | **500 VUs, one login each, all starting in the same instant.** Isolates the authentication path, which the mixed test spreads over the ramp. |

Each VU is a *distinct* synthetic account, so the run exercises real per-user
authorization and data isolation rather than 500 copies of one session.

Every request carries an explicit `name` tag, so latency is reported per flow rather
than as one blended number.

---

## 3. Results

### 3.1 The 500-user run

All rows are the same scenario against the same data set; only the server
configuration differs.

| Run | Configuration | Throughput | Error rate | Checks | p50 | p95 | p99 | max |
|---|---|---|---|---|---|---|---|---|
| **1** | pool **20**, default connection release | 293.70 req/s | **0.000 %** | 100 % | n/a¹ | **521 ms** | n/a¹ | 2966 ms |
| 2 | pool 20, release **after every statement** | 264.04 req/s | 0.450 % ✗ | 98.9 % ✗ | 51 ms | 900 ms ✗ | 1783 ms | 15617 ms |
| 3 | pool 20², release **at transaction end** | 267.92 req/s | 0.000 % | 100 % | 98 ms | 754 ms | 1540 ms | 13977 ms |
| **4** | pool **40**, default connection release | **328.57 req/s** | **0.000 %** | 100 % | **5 ms** | **9 ms** | **13 ms** | 520 ms |
| ~~5~~ | pool 20, default release | *invalid³* | — | — | — | — | — | — |
| **6** | pool **40** + notification write-path fix | **328.35 req/s** | **0.000 %** | 100 % | **5 ms** | **10 ms** | **15 ms** | **166 ms** |

¹ Run 1 was executed before the summary was extended to compute the median and p99
(k6 only computes the percentiles it is asked for), so those two cells were reported
as `NaN`/`0` by k6 and are not quoted.

² Run 3 was *intended* to test a pool of 40. `application-loadtest.properties`
silently overrode the value back to 20, so the run actually measured a pool of 20 —
which is why it is listed as such. The override has since been removed.

³ Run 5 was destroyed by a host freeze: HikariCP reported
`Thread starvation or clock leap detected (housekeeper delta=14m38s)`, i.e. the JVM
received no CPU for 14.6 minutes. It is excluded rather than used as evidence.

### 3.2 Final per-flow latency (run 6: pool 40 + write-path fix)

500 peak VUs, 110,182 requests, 20,582 iterations, 0 interrupted.

| Flow | p50 | p95 | p99 | max | budget | met |
|---|---|---|---|---|---|---|
| all requests | 5 ms | 10 ms | 15 ms | 166 ms | p95 < 1500 ms | ✅ |
| login | 4 ms | 10 ms | 15 ms | 50 ms | p95 < 1200 ms | ✅ |
| dashboard (own proposals) | 7 ms | 12 ms | 17 ms | 59 ms | p95 < 900 ms | ✅ |
| proposal list (status filter) | 4 ms | 9 ms | 14 ms | 70 ms | p95 < 900 ms | ✅ |
| proposal detail | 5 ms | 8 ms | 13 ms | 81 ms | p95 < 900 ms | ✅ |
| notifications | 3 ms | 6 ms | 11 ms | 87 ms | p95 < 900 ms | ✅ |
| calls for proposals | 2 ms | 4 ms | 10 ms | 72 ms | p95 < 900 ms | ✅ |
| proposal submit | 7 ms | 13 ms | 27 ms | 166 ms | p95 < 2000 ms | ✅ |

Run 6 reproduces run 4 (328.35 vs 328.57 req/s; p95 10 vs 9 ms), so the result is
stable rather than a lucky sample. The write-path fix shows up in the tail: `max`
fell from 520 ms to 166 ms.

### 3.3 500 simultaneous logins (login burst, final configuration)

| Metric | Value |
|---|---|
| logins succeeded | **500 of 500** |
| error rate | **0.000 %** |
| p50 / p95 / p99 / max | **475 / 501 / 508 / 556 ms** |

### 3.4 Server resources during the runs

| Metric | Run 1 (pool 20) | Run 4 (pool 40) |
|---|---|---|
| JVM CPU used | 1.13 of 8 cores (14 %) | 0.93 of 8 cores (12 %) |
| JVM RSS peak | 520 MB | 417 MB |
| JDBC connections checked out | **20 of 20 (100 %)** | 18 of 40 (45 %) |
| Connections actually executing a query | max 6, **avg 0.4** | max 4, **avg 0.3** |
| Connections `idle in transaction` | max 4 | 0 |
| PostgreSQL lock waits | 0 | 0 |
| Longest running query | < 1 s | < 1 s |

---


## 4. Bottlenecks found, and what was done

### 4.1 The JDBC connection pool was the limit — not the CPU, not PostgreSQL

The resource data says it unambiguously: at 500 users the JVM used **14 % of one
core's worth of CPU** (1.13 of 8), PostgreSQL had **no lock waits and no slow
queries**, and only **0.4 connections on average were executing a query** — while
**all 20 pool connections were checked out**. The requests were not waiting for the
database; they were waiting for a *connection to the database*.

Little's Law confirms it and explains the 58× difference between runs 1 and 4:

```
run 1:  20 connections / 293.7 req/s  -> mean connection hold time 68 ms
        capacity of 20 connections at 68 ms  = 20 / 0.068 = 294 req/s
        observed throughput                  = 293.7 req/s   <- exactly at capacity
        => every extra request queued        => p95 521 ms

run 4:  18 connections / 328.6 req/s  -> mean connection hold time 55 ms
        capacity of 20 connections at 55 ms  = 20 / 0.055 = 364 req/s
        observed throughput                  = 328.6 req/s   <- 90 % of capacity
        => no queueing                       => p95 9 ms
```

Run 1 sat exactly on the pool's capacity, so its latency was queue wait, not service
time.

**Change:** the pool was sized from that measurement rather than guessed —
`spring.datasource.hikari.maximum-pool-size=40`, giving ~727 req/s of capacity at the
measured 55 ms hold time, i.e. roughly 2× the throughput measured at 500 users. It is
not oversized: PostgreSQL allows 100 connections here, and 40 leaves 60 for `psql`,
the test suite, monitoring and a second instance. The sizing rule and the arithmetic
are written next to the value in `application.properties`.

### 4.2 Two "obvious" connection-release optimisations both made it worse

Both were implemented, measured at 500 users and **reverted**, because the
measurement contradicted the theory:

| Connection release mode | Throughput | Errors | p95 |
|---|---|---|---|
| default (hold for the request) | **293.70 req/s** | **0.000 %** | **521 ms** |
| release after every statement | 264.04 req/s | 0.450 % | 900 ms |
| release at transaction end | 267.92 req/s | 0.000 % | 754 ms |

A proposal-list request issues ~8 statements and also lazy-loads after the transaction
has ended (the session stays open while the response is serialized). Releasing the
connection repeatedly turns those into repeated acquire/release cycles against an
already contended pool, which multiplies queueing. The setting is therefore left at
the Hibernate default, with this table recorded in `application.properties` so it is
not "optimised" again without re-measuring.

### 4.3 A proposal write loaded the entire user table

`ProposalService.notifySubscribedRoles` began with `userRepository.findAll()`: every
status change loaded **every account in the system** and iterated all of them to find
the two or three that matched, and then each notification looked its recipient up
again by id. With 518 accounts that was ~500 rows of loading per proposal write, and
the cost grows linearly with the user table — the wrong shape for a university-wide
system.

**Change:** `UserRepository.findByRoleIn(...)` fetches only the accounts the status
concerns, in one indexed query (`idx_users_role`), and
`NotificationService.createNotificationForUser(user, ...)` writes to the
already-loaded account instead of re-reading it. The `switch` that decides what each
status notifies was left untouched, so the behaviour is unchanged; only the candidate
set was narrowed. Effect on the tail: `max` 520 ms → 166 ms.

### 4.4 500 simultaneous connections overflowed the accept queue

The login-burst scenario failed on its first attempt: **109 of 500 logins were
refused** (21.8 % error rate, `connectex: No connection could be made because the
target machine actively refused it`). Tomcat passes `server.tomcat.accept-count` to
the server-socket bind, so it is also the operating system's listen backlog; at 300 it
was not large enough for 500 connections arriving in the same instant.

**Change:** `server.tomcat.accept-count=1000`, `server.tomcat.max-connections=4000`.
The burst then succeeded 500/500 with a 0 % error rate.

### 4.5 Per-request work removed before any of the above

Removed because it is pure waste on every request, and it matters most exactly when
500 of them happen at once:

| Removed | Where | Why it mattered |
|---|---|---|
| 6 × `System.out.println` per request | `TokenAuthenticationFilter` | `System.out` is one synchronized stream; the largest avoidable per-request cost |
| 11 × `System.out.println` per login attempt, **including the submitted and stored passwords** | `UserService.login` | a credential disclosure in the server log *and* a per-request bottleneck |
| SQL logging on in every environment | `spring.jpa.show-sql` / `format_sql` | every statement of every request was formatted and printed; now off by default, on in the `dev` profile |
| Three base64 decodes of the token per request | `TokenAuthenticationFilter` + `TokenService` | the token is now parsed once per request |
| N+1 lazy loading on the 7 proposal child collections | `application.properties` | `hibernate.default_batch_fetch_size=50` collapses one SELECT per collection per row into one per 50 rows |

### 4.6 Checked and confirmed NOT to be bottlenecks

- **PostgreSQL**: 0 lock waits, longest query < 1 s, avg 0.4 active connections.
- **Pessimistic row locks** (revision resubmission, endorsement approval): no lock
  waits at 500 users, so the concurrency control is not contended.
- **Tomcat worker threads**: peak 163 threads against 120 worker threads plus JVM
  internals — never exhausted.
- **CPU**: 14 % of one core's worth. The backend is I/O-bound, not CPU-bound.

---


## 5. Synthetic data set (`rpis_test` only)

Generated by `SyntheticTestDataSeeder` (`loadtest` profile); deterministic
(`new Random(20260101L)`) and idempotent.

| Table | Rows |
|---|---|
| `users` (synthetic) | 518 (500 proponents + 12 Deans + 6 office accounts) |
| `proposals` (synthetic) | 1200 |
| — distinct statuses | **16** |
| — distinct creation **months** | **40** |
| — distinct creation **years** | **4** |
| `proposal_versions` | 1738 |
| `proposal_reviews` | 1359 |
| `endorsement_requests` | 220 |
| `endorsement_request_events` | 881 |
| `certifications` | 147 |
| `notifications` | 3000 |
| `quarterly_progress_reports` | 140 |
| `proposal_announcements` | 12 |

Timestamps are spread over ~3.5 years. `@CreationTimestamp` overwrites any value set
before an insert, so the spread is applied by one `UPDATE` per table **restricted to
synthetic rows** — no genuine record is ever touched.

### How the synthetic rows are marked

- accounts: `loadtest.<role>.<nnnn>@msunaawan.edu.ph` and a `[SYNTHETIC]` display name.
  The domain has to stay `@msunaawan.edu.ph` because the login endpoint only accepts
  it; the `loadtest.` local part is the marker.
- proposals, notifications and endorsements: `[SYNTHETIC]` prefix in the title.
- `synthetic_seed_registry`: the batch marker row that makes reruns idempotent.

---

## 6. Verification

| Check | Result |
|---|---|
| Backend test suite (`mvn test`) | **450 passed, 0 failures, 0 errors** |
| Frontend test suite (`npm test`) | **374 passed, 0 failed** |
| Frontend build (`npm run build`) | succeeds |
| k6 thresholds, 500-user run (run 6) | **all passed** |
| k6 thresholds, login burst (final) | **all passed** |
| `rpis_db` synthetic users | **0** |
| `rpis_db` synthetic proposals | **0** |
| `rpis_db` `synthetic_seed_registry` table | **does not exist** |
| `rpis_db` row counts | unchanged: 24 users / 25 proposals / 1027 notifications / 35 versions / 15 reports |
| Seeder rerun with data present | skips, writes nothing |
| Seeder rerun after the schema was recreated | regenerates the identical data set |

---

## 7. Remaining limitations

These are the limits of what was measured. None of them is a claim that the system
does not work; they are the things this evidence does **not** cover.

1. ~~**Only one backend instance was measured.**~~ **Closed.** The single-instance
   figures in section 3 stand as they were measured; a two-instance run behind a load
   balancer has since been performed and is reported separately in **section 8**. The
   pool rule `instances × pool <= max_connections - reserved` is documented in
   `backend/DEPLOYMENT.md` section 6 and was used to size that run.
2. **The load generator shares the host with the server**, so both compete for the
   same 8 cores. The figures are therefore conservative.
3. **The host froze twice during the campaign** (HikariCP reported 14.6-minute and
   36-minute thread-starvation gaps). Run 5 was destroyed by this and is excluded.
   The reported runs were verified free of that warning, but the host is not a
   controlled benchmark environment.
4. **`mvn test` resets `rpis_test`.** The `test` profile uses
   `ddl-auto=create-drop`, so the load-test data must be regenerated after a test run.
   The seeder now detects this (marker present, data absent) and rebuilds
   automatically. Separate databases for the two profiles would be cleaner.
5. **Pagination was not added to the list endpoints.** Pool sizing removed the
   measured bottleneck without it, and adding it would change existing responses. The
   RPS master-monitoring view still loads every proposal in one response; at 1200
   proposals that is fine, and it is the next thing to change if the table grows by an
   order of magnitude.
6. **`spring.jpa.open-in-view` remains enabled.** Disabling it would remove the
   connection hold during serialization but would break every endpoint that relies on
   lazy collection serialization. It was deliberately not attempted; the N+1 cost is
   handled by batch fetching instead.
7. **`npm run type-check` was already broken before this work** — the `vue-tsc` script
   points at a `tsconfig.json` that does not exist in the repository. The project is
   plain JavaScript and `npm run build` succeeds.
8. ~~**Passwords are still stored and compared in plain text.**~~ **Closed.** Passwords
   are now BCrypt hashes, with a lazy upgrade on successful sign-in and an opt-in bulk
   migration guarded by a cluster lock (see `backend/DEPLOYMENT.md` section 9). The
   cost of that change is measured honestly in section 8.5, where the login burst now
   breaches its threshold.
9. **File uploads were not load-tested.** The scenarios cover login, dashboard,
   proposal listing, notifications and submission as required; multi-megabyte uploads
   under 500-way concurrency were not measured.


---

## 8. Multi-instance verification (added after section 7 was written)

Section 7 recorded that only one instance had been measured. That gap has since been closed.
**These results are separate from the single-instance baselines in section 3 and do not
replace them:** section 3 measures one instance on port 8082; this section measures two
instances behind a load balancer.

### 8.1 Setup

| Item | Value |
|---|---|
| Instances | two, ports 8082 and 8083, same host |
| Database | the same `rpis_test` for both |
| Token secret | identical on both (a throwaway value for the run, never written to disk) |
| Upload directory | identical on both (`RPIS_UPLOAD_DIR`) |
| Pool | `RPIS_DB_POOL_SIZE=30` per instance, so 60 of the 97 usable connections are configured |
| Load balancer | `load-tests/lb/rpis-lb.mjs`, round-robin, port 8080, health probe `/api/health/ready` |
| Load generator | k6 v0.54.0 on the same host |
| Data set | 518 accounts, 1200 proposals |

The balancer is a **dev-grade Node proxy on the same host**, not a production balancer: it
is an extra hop and an extra consumer of the same 8 cores. The two-instance figures
therefore include that overhead and are conservative.

### 8.2 The 500-user run through the load balancer

| Metric | Single instance (section 3, run 6) | Two instances behind the LB |
|---|---|---|
| Throughput | 328.35 req/s | **323.27 req/s** |
| Error rate | 0.000 % | **0.000 %** |
| Checks passed | 100 % | **100 %** |
| Requests | 110,182 | 108,376 |
| p50 / p95 / p99 / max (all) | 5 / 10 / 15 / 166 ms | **5 / 10 / 61 / 7152 ms** |
| k6 thresholds | all passed | **all passed** |

Per flow:

| Flow | p50 | p95 | p99 | max | budget | met |
|---|---|---|---|---|---|---|
| login | 67 | 108 | 135 | 154 | p95 < 1200 | yes |
| dashboard | 7 | 11 | 27 | 6944 | p95 < 900 | yes |
| proposal list | 5 | 9 | 27 | 7152 | p95 < 900 | yes |
| proposal detail | 4 | 8 | 20 | 1147 | p95 < 900 | yes |
| notifications | 3 | 7 | 28 | 1896 | p95 < 900 | yes |
| calls for proposals | 2 | 5 | 25 | 1898 | p95 < 900 | yes |
| submit | 5 | 9 | 17 | 43 | p95 < 2000 | yes |

Throughput is within 1.6 % of the single-instance figure, at half the VUs per instance. The
**p99 and max are worse** (15 -> 61 ms, 166 -> 7152 ms). The tail is not explained by either
instance CPU (4.3 % and 4.7 % of the host) nor by the database (no lock waits, at most 4
connections executing a query). The likely contributors are the extra proxy hop and the
multi-second scheduler stalls described in 8.6.

### 8.3 Traffic distribution

The balancer served **106,086 requests to each instance** (a 50.0 / 50.0 split), with **0
errors** on both. Those counters are cumulative over the session, which included one
discarded run; the valid run alone split 54,438 / 54,439.

### 8.4 Per-instance resources

| Metric | Instance 8082 | Instance 8083 |
|---|---|---|
| JVM CPU | 135.2 s over 395 s = 0.34 of 8 cores (4.3 %) | 149.0 s over 395 s = 0.38 of 8 cores (4.7 %) |
| RSS | 407 - 412 MB | 395 - 441 MB |
| Threads (peak) | 103 | 109 |
| PostgreSQL lock waits | 0 | 0 |

The `pg_*` columns in `resources.csv` are **database-wide**, so both instances record the
same value. Peak connections to `rpis_test` were **31** across both instances, against 60
configured (30 each) and 97 usable. The pool was therefore not the constraint.
### 8.5 The 500-login burst after BCrypt was enabled

| Metric | Before BCrypt (section 3.3) | After BCrypt |
|---|---|---|
| logins succeeded | 500 of 500 | **500 of 500** |
| error rate | 0.000 % | **0.000 %** |
| p50 / p95 / p99 / max | 475 / 501 / 508 / 556 ms | **2950 / 5302 / 5329 / 5386 ms** |
| wall-clock for all 500 | not recorded | **5.5 s** |

**The `login` threshold is BREACHED.** `http_req_duration{name:login}` is
`p(95)<1500, p(99)<3000`; the measured p95 is 5302 ms and the p99 is 5329 ms, and k6
reported `thresholds on metrics 'http_req_duration{name:login}' have been crossed`.

This is recorded as a **failed acceptance criterion**, not a passing one. The threshold
predates BCrypt and was deliberately **not** adjusted: widening a threshold after seeing
the result would make the number meaningless.

All 500 logins still succeeded, so the system does handle 500 simultaneous logins; it is
the latency budget that is not met. The cause is arithmetic rather than a defect - BCrypt
at strength 10 costs roughly 60-80 ms of CPU per verification, 500 verifications cannot be
parallelised away, and the host has 8 cores shared with the generator, both JVMs, the
balancer and PostgreSQL.

Mitigations are proposed in section 9. **None has been applied.**

### 8.6 A run that was discarded

The first 500-user run through the balancer measured 307.07 req/s with an **18,141 ms
maximum** on dashboard and notifications. Both JVMs logged

```
HikariPool-1 - Thread starvation or clock leap detected (housekeeper delta=47s729ms857us900ns)
```

that is, the JVMs received no CPU for about 47 seconds, and the 18-second maximum is that
stall. By the rule in section 7.3 this is **not a valid measurement** and it is excluded.
Its artefacts are kept as `INVALID-frozen-run-500u-*` so the exclusion is auditable.

The login burst and the second 500-user run both completed with no such warning, so both
are reported.

### 8.7 A defect this work found and fixed

Starting two instances together against an **empty** database aborted one of them:

```
ERROR: duplicate key value violates unique constraint "uk_priority_agenda_set_framework_period"
Detail: Key (lower(framework_name::text), coverage_start_year, coverage_end_year)=(msun riide, 2025, 2028) already exists.
Application run failed
```

Two components create the same reference row on start-up, each with a check-then-act race,
and they raced **each other**: `PriorityAgendaInitializer` (runs on every instance, no
lock) and `SyntheticTestDataSeeder` (only in the `loadtest` profile, no lock on that step).
A JVM-level lock cannot help, because they are different processes.

Both now take the same PostgreSQL advisory lock (`pg_try_advisory_lock`, namespace
`"RPIS"`, purpose 3), so only one writer can be between its existence check and its
insert. The seeder also reuses an existing framework instead of assuming it created one,
and the initializer insert is `ON CONFLICT DO NOTHING`. Re-running the simultaneous start
produced exactly one data set:

```
users=518  proposals=1200  priority_agenda_sets=1  priority_agendas=6  duplicate_emails=0
```

The same mechanism now guards the plain-text password migration (purpose 1) and the
synthetic generation itself (purpose 2).

### 8.8 Concurrent `ddl-auto=update`

With two instances starting together against an empty schema, both issue `CREATE TABLE`
for the same tables and the loser logs `relation "..." already exists`. Hibernate treats
this as a warning rather than a failure, so both instances came up and nothing was lost -
but the log is noisy and the outcome relies on that tolerance. Production uses
`ddl-auto=none` with the schema applied beforehand (see `backend/DEPLOYMENT.md`), so this
does not arise there.

---

## 9. Proposed mitigations for the login burst (NOT APPLIED)

Recorded for review. **None of these has been implemented and the threshold has not been
changed.** Ordered by benefit relative to risk.

1. **Rate-limit the login endpoint.** A per-IP and per-account token bucket (say 10
   attempts per minute) turns a 500-way burst into a queue instead of a stampede, and it
   is the only option here that also reduces the value of a brute-force attempt. It needs
   a store shared by all instances (the database, or Redis), or it becomes a per-instance
   limit that a load balancer defeats. Risk: users behind one campus NAT could be
   throttled together.
2. **Bound the concurrency of password verification.** A bounded semaphore sized to the
   core count in front of `PasswordService.verify` would stop 500 verifications competing
   for 8 cores. Same total work, a much better p95, no account lockout. Risk: a login
   flood would then delay other users' logins.
3. **Restate the threshold with evidence.** `p95 < 1500 ms` was chosen when verification
   was a string comparison. If BCrypt stays at strength 10, the budget should be restated
   in terms of what the design can deliver - for example "500 simultaneous logins all
   succeed within 10 s, p95 under 6 s" - and agreed by the people who agreed the
   original. A policy decision, not a code change.
4. **Tune the resource envelope rather than the algorithm.** The run used 0.72 of 8 cores
   in total, so the host was not saturated; the latency is queueing behind a fixed amount
   of BCrypt work. More cores, or a pool dedicated to the login path, would raise the
   ceiling without weakening the hash.

**Deliberately rejected: lowering the BCrypt cost factor, and caching successful
comparisons.** Lowering the cost weakens every stored password to make a synthetic
benchmark look better. Caching a successful comparison would let a second login with the
same credentials skip verification, which removes the property that makes the hash useful
- and it would make this measurement a lie, because the burst would no longer be doing 500
real verifications.