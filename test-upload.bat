@echo off
echo Testing file upload to Knowledge Graph API...
echo.

REM Upload a test CSV file
curl -X POST http://localhost:8080/api/ingest/upload ^
  -F "file=@test-files/test.csv" ^
  -H "Accept: application/json"

echo.
echo Upload complete! Check the response above.
pause