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
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
