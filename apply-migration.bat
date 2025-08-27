@echo off
echo Applying database migration to convert enums to VARCHAR...

set PGPASSWORD=postgres
psql -U postgres -h localhost -d knowledge_graph -f database/migrations/V2__convert_enums_to_varchar.sql

if %ERRORLEVEL% == 0 (
    echo Migration applied successfully!
) else (
    echo Migration failed. Error code: %ERRORLEVEL%
)
pause