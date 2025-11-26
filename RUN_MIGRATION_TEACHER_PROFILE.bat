@echo off
REM Migration script for adding teacher profile fields
REM Run this script to add linkedin_url, facebook_url, and bio columns to teachers table

echo ====================================
echo Running Teacher Profile Migration
echo ====================================
echo.

REM Set MySQL credentials (update these if needed)
set MYSQL_USER=root
set MYSQL_PASS=123456
set MYSQL_DB=edu360
set MYSQL_HOST=localhost
set MYSQL_PORT=3306

echo Connecting to MySQL...
echo Database: %MYSQL_DB%
echo Host: %MYSQL_HOST%:%MYSQL_PORT%
echo.

REM Run the migration SQL file
mysql -u %MYSQL_USER% -p%MYSQL_PASS% -h %MYSQL_HOST% -P %MYSQL_PORT% %MYSQL_DB% < MIGRATION_ADD_TEACHER_PROFILE_FIELDS.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================
    echo Migration completed successfully!
    echo ====================================
    echo.
    echo New columns added to teachers table:
    echo - linkedin_url VARCHAR(500)
    echo - facebook_url VARCHAR(500)
    echo - bio TEXT
    echo.
) else (
    echo.
    echo ====================================
    echo Migration failed!
    echo ====================================
    echo Please check the error messages above.
    echo.
)

pause
