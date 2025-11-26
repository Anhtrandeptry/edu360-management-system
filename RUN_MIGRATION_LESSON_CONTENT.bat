@echo off
chcp 65001 >nul
echo ========================================
echo  Migration: Add lesson_content column
echo ========================================
echo.

REM Prompt for MySQL credentials
set /p MYSQL_USER="Enter MySQL username [root]: "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD="Enter MySQL password: "

set MYSQL_DB=edu360_system
set MIGRATION_FILE=MIGRATION_ADD_LESSON_CONTENT.sql

echo.
echo Running migration from %MIGRATION_FILE%...
echo Database: %MYSQL_DB%
echo.

mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DB% < %MIGRATION_FILE%

if errorlevel 1 (
    echo.
    echo ❌ Migration FAILED!
    echo Please check:
    echo   1. MySQL credentials
    echo   2. Database name: %MYSQL_DB%
    echo   3. SQL file syntax
    pause
    exit /b 1
) else (
    echo.
    echo ✅ Migration completed successfully!
    echo.
    echo The lesson_content column has been added to class_sessions table.
    echo Teachers can now save lesson content when marking attendance.
    echo.
    pause
)
