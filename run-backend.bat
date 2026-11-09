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
