@echo off
echo Setting up PostgreSQL database...
echo.
echo Please enter the postgres user password when prompted.
echo.

psql -U postgres -h localhost -f database/init.sql

echo.
echo Database setup complete!
pause