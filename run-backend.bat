@echo off
rem Keep the environment local: the port check below sets its own variables, and they
rem must not leak into the console this script was launched from.
setlocal
echo Starting the Spring Boot Backend (dev profile)...
rem Use the script's own directory, so double-clicking the file or running it from
rem another working directory still starts the right project.
cd /d "%~dp0backend"
if errorlevel 1 (
    echo Could not change into the backend directory next to this script.
    exit /b 1
)

rem ---- Java -------------------------------------------------------------------
rem Prefer the JDK bundled beside the backend directory. If it is not there (it is
rem not committed), fall back to the machine's JAVA_HOME / PATH instead of pointing
rem JAVA_HOME at a directory that does not exist, which made Maven fail before
rem Spring Boot even started.
if exist "%CD%\jdk-17.0.11+9\bin\java.exe" (
    set "JAVA_HOME=%CD%\jdk-17.0.11+9"
) else (
    echo Bundled JDK not found - using the system JAVA_HOME / java.
)
set "JAVA_OK="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_OK=1"
if not defined JAVA_OK (
    where java >nul 2>&1 && set "JAVA_OK=1"
)
if not defined JAVA_OK (
    echo Java not found. Install a JDK 17+ or place jdk-17.0.11+9 beside the backend directory.
    exit /b 1
)

rem The "dev" profile restores the local development behaviour: SQL logging on, a small
rem connection pool and the demo accounts seeded on start-up. The schema itself is owned
rem by backend/migration-*.sql (ddl-auto=none), so a missing migration fails start-up
rem instead of being silently created by Hibernate.
set SPRING_PROFILES_ACTIVE=dev

rem ---- Database connection ---------------------------------------------------
rem RPIS_DB_URL is REQUIRED by application.properties and has NO default: without it
rem start-up stops with "Could not resolve placeholder 'RPIS_DB_URL'". The values
rem below are the local development defaults; "if not defined" keeps anything already
rem exported in the environment (changed credentials, a different host) as the override.
if not defined RPIS_DB_URL set "RPIS_DB_URL=jdbc:postgresql://localhost:5432/rpis_db"
if not defined RPIS_DB_USER set "RPIS_DB_USER=postgres"
if not defined RPIS_DB_PASSWORD set "RPIS_DB_PASSWORD=1234"

rem ---- Database schema ---------------------------------------------------------
rem This launcher is the local development entry point, so the idempotent
rem backend/migration-*.sql set is applied before Spring Boot starts. A full
rem re-run takes under a second on a local database and never rewrites rows.
rem Set RPIS_SKIP_MIGRATIONS=1 to start without touching the schema.
if /i "%RPIS_SKIP_MIGRATIONS%"=="1" (
    echo Database migrations skipped - RPIS_SKIP_MIGRATIONS is set.
) else (
    call :apply_migrations
    if errorlevel 1 exit /b 1
)

rem The start-up guard now expects the single database name rpis_db by default, so no
rem RPIS_DB_EXPECTED is needed unless the target clone is deliberately named otherwise.
rem State the listening port explicitly instead of inheriting the shared default in
rem application.properties. The front end reads VITE_API_BASE_URL from .env.local and
rem must agree with this value, otherwise every API call fails with
rem net::ERR_CONNECTION_REFUSED. 8081 is the dev / no-profile default; 8082 belongs to
rem the `loadtest` profile (see load-tests/start-instance.ps1). "if not defined" keeps
rem an explicitly exported RPIS_SERVER_PORT working as an override.
if not defined RPIS_SERVER_PORT set RPIS_SERVER_PORT=8081
rem ---- Clear the port before Maven starts anything ---------------------------
rem spring-boot:run forks a child JVM, and closing the console window does not always
rem take that child with it. The leftover keeps LISTENING on %RPIS_SERVER_PORT%, and the
rem next start then fails with
rem   "Web server failed to start. Port 8081 was already in use."
rem which is exactly the state this repository was in when the check was added, so the
rem port is cleared here instead of being discovered by Tomcat several seconds later.
rem
rem Only the port this script binds is touched. 8082 is the `loadtest` profile
rem (load-tests/start-instance.ps1) and is meant to run ALONGSIDE a dev instance, 5173
rem is the Vite dev server and 5432 is PostgreSQL - none of those are this script's
rem business.
rem
rem RPIS_SKIP_PORT_CHECK=1 skips the check, for the case where the port is deliberately
rem shared with another process or the caller has already dealt with it.
if not defined RPIS_SKIP_PORT_CHECK call :free_port %RPIS_SERVER_PORT%
if errorlevel 1 exit /b 1

rem ---- Maven -------------------------------------------------------------------
rem Prefer the Maven bundled beside the backend directory; otherwise use the one on
rem PATH. Checking first turns "the system cannot find the path specified" into a
rem readable message.
if exist "%CD%\apache-maven-3.9.6\bin\mvn.cmd" (
    set "MVN_CMD=%CD%\apache-maven-3.9.6\bin\mvn.cmd"
) else (
    set "MVN_CMD=mvn"
)
set "MVN_OK="
if /i "%MVN_CMD%"=="mvn" (
    where mvn >nul 2>&1 && set "MVN_OK=1"
) else (
    if exist "%MVN_CMD%" set "MVN_OK=1"
)
if not defined MVN_OK (
    echo Maven not found. Place apache-maven-3.9.6 beside the backend directory or put mvn on PATH.
    exit /b 1
)

"%MVN_CMD%" spring-boot:run
rem Propagate Maven's exit code, so a terminal task or a caller can tell a failed
rem start-up from a successful one.
exit /b %ERRORLEVEL%

rem =============================================================================
rem :apply_migrations
rem -----------------------------------------------------------------------------
rem Applies the verified order from backend/DEPLOYMENT.md. Every file is
rem idempotent (ADD COLUMN / TABLE / INDEX IF NOT EXISTS, or guarded DO blocks),
rem so the list is safe on every start. The JDBC URL is parsed for host / port /
rem database, and RPIS_DB_USER / RPIS_DB_PASSWORD are reused as psql credentials.
rem psql runs quietly; its output goes to migration-run.log, which is deleted on
rem success and shown (tail only) on failure.
rem Returns 0 when the schema is current, 1 otherwise.
rem =============================================================================
:apply_migrations
setlocal

rem psql ships with PostgreSQL but is often not on PATH on Windows. Look on PATH
rem first, then in the standard PostgreSQL installation directories.
set "PSQL_CMD="
where psql >nul 2>&1 && set "PSQL_CMD=psql"
if not defined PSQL_CMD (
    for /d %%D in ("%ProgramFiles%\PostgreSQL\*") do (
        if exist "%%D\bin\psql.exe" set "PSQL_CMD=%%D\bin\psql.exe"
    )
)
if not defined PSQL_CMD (
    echo.
    echo Could not update the database: the psql command was not found.
    echo Install the PostgreSQL client tools, or start with RPIS_SKIP_MIGRATIONS=1.
    echo.
    pause
    endlocal
    exit /b 1
)

rem jdbc:postgresql://<host>:<port>/<database>[?params] -> PGHOST / PGPORT / PGDATABASE
rem Clear inherited PG* values first, so only this script's URL decides the target.
set "PGHOST="
set "PGPORT="
set "PGDATABASE="
set "DBURL=%RPIS_DB_URL%"
set "DBURL=%DBURL:jdbc:postgresql://=%"
for /f "tokens=1,* delims=/" %%A in ("%DBURL%") do (
    set "DBHOSTPORT=%%A"
    set "DBREST=%%B"
)
for /f "tokens=1 delims=?" %%A in ("%DBREST%") do set "PGDATABASE=%%A"
for /f "tokens=1,2 delims=:" %%A in ("%DBHOSTPORT%") do (
    set "PGHOST=%%A"
    set "PGPORT=%%B"
)
if not defined PGHOST set "PGHOST=localhost"
if not defined PGPORT set "PGPORT=5432"
if not defined PGDATABASE (
    echo Could not read the database name from RPIS_DB_URL: %RPIS_DB_URL%
    echo.
    pause
    endlocal
    exit /b 1
)
set "PGUSER=%RPIS_DB_USER%"
set "PGPASSWORD=%RPIS_DB_PASSWORD%"

echo Applying database migrations...
for %%F in (
    migration-rename-application-cycles-to-proposal-announcements.sql
    migration-add-proposal-announcements.sql
    migration-add-announcement-details.sql
    migration-add-announcement-funding-program.sql
    migration-add-proposal-announcement-updates.sql
    migration-add-notification-announcement-link.sql
    migration-add-budget-file-path.sql
    migration-add-cooperating-agencies.sql
    migration-add-endorsement-workflow.sql
    migration-add-endorsement-titles.sql
    migration-add-proposal-section-remarks.sql
    migration-add-field-level-remarks.sql
    migration-add-notification-fields.sql
    migration-add-notification-preferences.sql
    migration-add-priority-agendas.sql
    migration-add-proposal-address-contact.sql
    migration-add-proposal-announcement-link.sql
    migration-add-proposal-code-sequence.sql
    migration-add-proposal-id-fields.sql
    migration-add-proposal-versions.sql
    migration-add-proposal-version-unique.sql
    migration-add-quarterly-progress-reports.sql
    migration-add-quarterly-report-copied-from.sql
    migration-add-quarterly-report-documents.sql
    migration-add-references-json.sql
    migration-add-revision-request-fields.sql
    migration-add-risk-management-json.sql
    migration-add-proposal-endorsement-document.sql
    migration-add-proposal-certification.sql
    migration-add-technology-map-path.sql
    migration-add-terminal-report-content.sql
    migration-add-user-signature.sql
    migration-add-workplan-json.sql
    migration-rename-dean-role.sql
    migration-add-must-change-password.sql
    migration-add-dean-assignments.sql
    migration-add-gad-checklist.sql
    migration-add-activity-logs.sql
    migration-add-endorsement-document-code-unique.sql
    migration-add-proposal-needs-budget.sql
    migration-add-performance-indexes.sql
    migration-01-db-audit-integrity.sql
    migration-02-db-audit-dedupe.sql
    migration-03-db-audit-status-history.sql
    migration-norm-01-relational-integrity.sql
    migration-norm-02-relational-observability.sql
    migration-norm-03-fk-consolidation.sql
) do (
    if not exist "%%F" (
        echo Migration file missing: %%F
        echo.
        pause
        endlocal
        exit /b 1
    )
    "%PSQL_CMD%" -X -q -v ON_ERROR_STOP=1 -f "%%F" >> migration-run.log 2>&1
    if errorlevel 1 (
        echo.
        echo Database migration failed: %%F
        echo The error is in %CD%\migration-run.log - the last lines are:
        powershell -NoProfile -Command "Get-Content -Tail 15 -LiteralPath 'migration-run.log'" 2>nul
        if errorlevel 1 type migration-run.log
        echo.
        pause
        endlocal
        exit /b 1
    )
)
del /q migration-run.log 2>nul
echo Database schema is up to date.
endlocal
exit /b 0

rem =============================================================================
rem :free_port <port>
rem -----------------------------------------------------------------------------
rem Stops every process LISTENING on <port>, then waits for the socket to be released.
rem
rem netstat is used rather than PowerShell: it exists on every Windows install, needs
rem no execution policy, and prints one line per listening socket with the owning PID
rem as the last field. The port is anchored with a trailing space so 8081 cannot match
rem 80810, and the lines are filtered to LISTENING so an outbound connection that
rem merely uses the port as its source is left alone.
rem
rem One PID appears once per address family (0.0.0.0 and [::]), so the same PID can be
rem killed twice; the second taskkill finds nothing and its error is silenced.
rem
rem Returns 0 when the port is free, 1 when it could not be freed.
rem =============================================================================
:free_port
setlocal
set "PORT=%~1"
set "KILLED="
for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr /R /C:":%PORT% " ^| findstr /R /C:"LISTENING"') do (
    echo Port %PORT% is in use by PID %%P - stopping it before start-up...
    taskkill /F /T /PID %%P >nul 2>&1
    set "KILLED=1"
)
if not defined KILLED (
    echo Port %PORT% is free.
    endlocal
    exit /b 0
)

rem The socket is released asynchronously: killing the JVM returns immediately, but
rem Windows can keep the port in the listener table for a moment afterwards. Poll
rem instead of assuming, so Maven does not start into a port that is still closing.
set /a WAITED=0
:free_port_wait
netstat -ano -p tcp | findstr /R /C:":%PORT% " | findstr /R /C:"LISTENING" >nul 2>&1
if errorlevel 1 (
    echo Port %PORT% is free now.
    endlocal
    exit /b 0
)
set /a WAITED+=1
if %WAITED% GEQ 10 (
    echo.
    echo Startup aborted: port %PORT% is still LISTENING after stopping its owner.
    echo Stop that process manually, or start again with RPIS_SKIP_PORT_CHECK=1.
    echo.
    pause
    endlocal
    exit /b 1
)
rem "ping -n 2" is a one-second sleep that, unlike "timeout", needs no console on
rem stdin - a redirected stdin makes "timeout" fail outright.
ping -n 2 127.0.0.1 >nul
goto :free_port_wait
