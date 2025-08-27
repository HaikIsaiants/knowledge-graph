@echo off
echo Starting Knowledge Graph Backend...
echo ==================================
echo.

REM Check if PostgreSQL is running
echo Checking PostgreSQL...
docker ps | findstr knowledge-graph-postgres >nul
if errorlevel 1 (
    echo PostgreSQL is not running. Starting it now...
    docker-compose up -d
    echo Waiting for PostgreSQL to be ready...
    timeout /t 10 /nobreak >nul
) else (
    echo PostgreSQL is running.
)

echo.
echo Building and starting Spring Boot application...
cd backend
call ..\tools\maven\bin\mvn spring-boot:run -Dspring-boot.run.profiles=local -Dmaven.test.skip=true -e