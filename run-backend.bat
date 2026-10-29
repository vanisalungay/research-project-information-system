@echo off
echo Starting the Spring Boot Backend (dev profile)...
cd backend
set JAVA_HOME=%CD%\jdk-17.0.11+9
rem The "dev" profile restores the previous local behaviour exactly: SQL logging on,
rem Hibernate updating the schema from the entities, a small connection pool and the
rem demo accounts seeded on start-up. Without a profile the conservative shared
rem settings apply instead (no SQL log, ddl-auto=none, no demo accounts), which is
rem what a real deployment should use.
set SPRING_PROFILES_ACTIVE=dev
rem State the listening port explicitly instead of inheriting the shared default in
rem application.properties. The front end reads VITE_API_BASE_URL from .env.local and
rem must agree with this value, otherwise every API call fails with
rem net::ERR_CONNECTION_REFUSED. 8081 is the dev / no-profile default; 8082 belongs to
rem the `loadtest` profile (see load-tests/start-instance.ps1). "if not defined" keeps
rem an explicitly exported RPIS_SERVER_PORT working as an override.
if not defined RPIS_SERVER_PORT set RPIS_SERVER_PORT=8081
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
