# AGENTS.md

RPIS: Vue 3 SPA + Spring Boot REST API + PostgreSQL.

## Architecture map

```
src/                        Frontend (Vue 3, Vite, JS + Pinia)
  main.js                   App entry
  App.vue
  router/index.js           All routes (every role/area)
  views/<Area>/             Pages: Admin, Auth, Dashboard, Dean, Notifications,
                            Oc, Ovc, Ovcaf, Proponent, Rec, Rps...
  components/               Reusable .vue components (44)
  composables/              Composition helpers (+ *.test.js)
  services/                 Axios/SSE clients (notificationStream.js)
  stores/                   Pinia stores (userData, notifications, deanAttention)
  utils/                    Pure logic + node:test files (*.test.js)
  assets/                   Images/fonts

backend/                    Spring Boot 3.2.5, Java 17, Maven
  pom.xml
  src/main/java/com/rpis/backend/
    controller/             REST endpoints, base path /api/* (e.g. /api/proposals)
    service/                Business logic, schedulers, notifications
    repository/             Spring Data JPA repositories (23)
    model/                  JPA entities + JSON converters
    dto/                    Request/response records
    config/ security/ storage/ util/ dev/
  src/main/resources/
    application.properties  Main config (RPIS_* env vars)
    application-{dev,prod,loadtest}.properties
    locations/, templates/  Static catalogs / letter templates
  src/test/java/            115 JUnit/Mockito integration tests
  schema.sql                Destructive reset: dev/test DBs only
  migration-*.sql           Ordered PostgreSQL migrations (apply in name order)

docs/                       ERD + functionality inventory
load-tests/                 k6 scenarios and runners
scripts/                    Node .mjs data/fetch utilities
public/                     Static assets, document templates
dist/, backend/target/      Build outputs (ignored)
uploads/, backend/uploads/  Runtime user files (ignored)
```

## Commands

```bash
# Frontend (repo root)
npm install
npm run dev               # Vite dev server
npm run build             # -> dist/
npm test                  # node --test on src/**/*.test.js
npm run type-check        # vue-tsc --build
npm run format            # prettier --write src/

# Lint: none configured. Use: npm run type-check && npx prettier --check src/

# Backend
cd backend
mvn spring-boot:run                       # port 8081
mvn clean package                         # -> target/backend-0.0.1-SNAPSHOT.jar
mvn test                                  # needs running PostgreSQL + rpis_test DB
mvn test -Dtest=ClassName                 # single test class
```

Tests: use JDK 17 (project target). JDK 25 breaks Mockito unit tests.

## Token-saving rules (mandatory)

1. Search before reading: use `rg`, Grep, or Glob to locate exact files/symbols.
   Never `ls -R`, `cat` wildcards, or scan whole directories blindly.
2. Read only needed ranges: use `Read` with `offset`/`limit` for files > ~400 lines.
   Grep with context (`-n -C`) before opening a file.
3. Never read lockfiles (`package-lock.json`), build outputs (`dist/`,
   `backend/target/`), logs (`*.log`), caches (`.aider*/`), or dependency folders
   (`node_modules/`). Same for `uploads/` runtime data unless explicitly asked.
4. Prefer targeted edits: locate the symbol, patch it, then verify with a narrow
   grep/test. Do not print full files back.
5. Keep responses brief: no filler, no restating the request, no exhaustive option
   surveys. Stop as soon as the requested edit is verified.
6. Always work directly on the `main` branch. Never create feature branches.
