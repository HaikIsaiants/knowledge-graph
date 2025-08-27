@echo off
echo Running backend tests...
cd backend

echo.
echo Compiling code and tests...
call mvnw.cmd clean compile test-compile

if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b %errorlevel%
)

echo.
echo Running tests...
call mvnw.cmd test

echo.
echo Tests completed!
pause