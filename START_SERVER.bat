@echo off
echo ===========================================
echo   Starting Edu360 Management System
echo ===========================================
echo.
echo Checking MySQL connection...
echo Make sure MySQL is running on localhost:3306
echo.

REM Change to the directory where this script is located
cd /d "%~dp0"

echo Current directory: %cd%
echo.
echo Starting Spring Boot application...
echo Backend will run on: http://localhost:8080
echo.

if defined MAVEN_HOME (
    call "%MAVEN_HOME%\bin\mvn" spring-boot:run
) else (
    call mvn spring-boot:run
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ===========================================
    echo   ERROR: Failed to start application
    echo ===========================================
    echo.
    pause
    exit /b %ERRORLEVEL%
)
