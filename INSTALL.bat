@echo off
echo =========================================
echo   Installing Project Dependencies
echo =========================================
echo.

cd /d "%~dp0"

echo Current Directory: %cd%
echo.
echo Downloading and installing all Maven dependencies...
echo This may take a few minutes on first run...
echo.

call "%MAVEN_HOME%\bin\mvn" clean install -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =========================================
    echo   Dependencies Installed Successfully!
    echo =========================================
    echo.
    echo Now you can run: START_SERVER.bat
) else (
    echo.
    echo =========================================
    echo   Installation Failed!
    echo =========================================
)

echo.
pause
