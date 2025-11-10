@echo off
REM ============================================================
REM Script: Check and Fix Database Schema for room_id columns
REM Purpose: Allow NULL values in room_id for online classes
REM ============================================================

echo.
echo ========================================
echo Checking Database Schema
echo ========================================
echo.

REM Check if MySQL is accessible
where mysql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] MySQL is not in PATH.
    echo Please add MySQL bin directory to PATH or run this script from MySQL bin directory.
    echo.
    echo Example: SET PATH=%%PATH%%;C:\Program Files\MySQL\MySQL Server 8.0\bin
    echo.
    pause
    exit /b 1
)

echo [INFO] MySQL found in PATH
echo.

REM Database credentials (adjust if needed)
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=edu360_system
set DB_USER=root
set DB_PASS=123456

echo [INFO] Connecting to database: %DB_NAME%
echo.

REM Check current schema for classes table
echo ========================================
echo Checking classes.room_id column...
echo ========================================
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -D%DB_NAME% -e "SHOW COLUMNS FROM classes LIKE 'room_id';" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to query classes table
    pause
    exit /b 1
)
echo.

REM Check current schema for class_sessions table
echo ========================================
echo Checking class_sessions.room_id column...
echo ========================================
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -D%DB_NAME% -e "SHOW COLUMNS FROM class_sessions LIKE 'room_id';" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to query class_sessions table
    pause
    exit /b 1
)
echo.

REM Ask user if they want to apply the migration
echo ========================================
echo Migration Actions
echo ========================================
echo.
echo The following changes will be applied:
echo   1. ALTER TABLE classes MODIFY COLUMN room_id BIGINT NULL
echo   2. ALTER TABLE class_sessions MODIFY COLUMN room_id BIGINT NULL
echo.
echo This will allow creating online classes without room assignments.
echo.
set /p CONFIRM="Do you want to apply these changes? (Y/N): "

if /i NOT "%CONFIRM%"=="Y" (
    echo.
    echo [INFO] Migration cancelled by user
    pause
    exit /b 0
)

echo.
echo ========================================
echo Applying Migration...
echo ========================================
echo.

REM Apply migration to classes table
echo [1/2] Modifying classes.room_id to allow NULL...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -D%DB_NAME% -e "ALTER TABLE classes MODIFY COLUMN room_id BIGINT NULL COMMENT 'Foreign key to rooms table. NULL for online classes';" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to modify classes.room_id
    pause
    exit /b 1
)
echo [SUCCESS] classes.room_id now allows NULL
echo.

REM Apply migration to class_sessions table
echo [2/2] Modifying class_sessions.room_id to allow NULL...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -D%DB_NAME% -e "ALTER TABLE class_sessions MODIFY COLUMN room_id BIGINT NULL COMMENT 'Foreign key to rooms table. NULL for online class sessions';" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to modify class_sessions.room_id
    pause
    exit /b 1
)
echo [SUCCESS] class_sessions.room_id now allows NULL
echo.

REM Verify changes
echo ========================================
echo Verifying Changes...
echo ========================================
echo.

echo [VERIFY] classes.room_id:
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -D%DB_NAME% -e "SHOW COLUMNS FROM classes LIKE 'room_id';" 2>nul
echo.

echo [VERIFY] class_sessions.room_id:
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -D%DB_NAME% -e "SHOW COLUMNS FROM class_sessions LIKE 'room_id';" 2>nul
echo.

echo ========================================
echo Migration Completed Successfully!
echo ========================================
echo.
echo Next steps:
echo   1. Restart your Spring Boot application
echo   2. Try creating an online class from the UI
echo.
pause
