@echo off
echo Starting the Spring Boot Backend...
cd backend
set JAVA_HOME=%CD%\jdk-17.0.11+9
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
