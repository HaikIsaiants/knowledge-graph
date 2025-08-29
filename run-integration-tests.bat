@echo off
echo ========================================
echo Running Integration Tests
echo ========================================
echo.

REM Make sure backend is compiled first
echo Building main project...
cd backend
call ..\tools\maven\bin\mvn.cmd clean install -DskipTests
if errorlevel 1 (
    echo Failed to build main project
    exit /b 1
)

echo.
echo Running integration tests...
call ..\tools\maven\bin\mvn.cmd test -f pom-integration.xml
if errorlevel 1 (
    echo Integration tests failed
    exit /b 1
)

echo.
echo ========================================
echo Integration tests completed!
echo ========================================