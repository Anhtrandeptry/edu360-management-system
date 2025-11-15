@echo off
echo ====================================
echo Fix Database Schema - Allow NULL semester_id
echo ====================================
echo.

REM Get database credentials from application.properties
REM Database name: edu360_system (from application.properties)
set DB_NAME=edu360_system
set DB_USER=root
set DB_PASS=123456

echo Connecting to database: %DB_NAME%
echo Running migration: MIGRATION_ALLOW_NULL_SEMESTER.sql
echo.

mysql -h localhost -u %DB_USER% -p%DB_PASS% %DB_NAME% < MIGRATION_ALLOW_NULL_SEMESTER.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================
    echo Migration completed successfully!
    echo ====================================
    echo.
    echo Please restart your Spring Boot application.
) else (
    echo.
    echo ====================================
    echo Migration FAILED!
    echo ====================================
    echo Please check the error messages above.
)

pause
