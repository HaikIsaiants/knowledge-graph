@echo off
echo ==========================================
echo    Knowledge Graph - Full Stack Startup
echo ==========================================
echo.

REM Check PostgreSQL
echo [1/3] Checking PostgreSQL database...
powershell -Command "if ((Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue).TcpTestSucceeded) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    echo PostgreSQL is not running on port 5432.
    echo Please start PostgreSQL manually or ensure it's running as a service.
    pause
    exit /b 1
)
echo PostgreSQL is running!

REM Start Backend in new window
echo.
echo [2/3] Starting Spring Boot backend...
start "Knowledge Graph - Backend" cmd /k run-backend.bat

echo Waiting for backend to start...
timeout /t 20 /nobreak >nul

REM Start Frontend in new window
echo.
echo [3/3] Starting Vue frontend...
start "Knowledge Graph - Frontend" cmd /k run-frontend.bat

echo.
echo ==========================================
echo All services are starting!
echo ==========================================
echo.
echo Services will be available at:
echo - Frontend:  http://localhost:3000
echo - Backend:   http://localhost:8080/api
echo - Database:  localhost:5432
echo.
echo Press any key to open the application in your browser...
pause >nul

start http://localhost:3000