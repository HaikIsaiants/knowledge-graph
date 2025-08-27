@echo off
echo Starting Knowledge Graph Frontend...
echo ===================================
echo.

cd frontend

REM Check if node_modules exists
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
)

echo Starting Vue development server...
call npm run dev