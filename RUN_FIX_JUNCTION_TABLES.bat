@echo off
chcp 65001 >nul
echo ========================================
echo  FIX: Add ID columns to junction tables
echo ========================================
echo.
echo Bảng session_lessons và session_chapters
echo hiện không có cột id, cần thêm vào!
echo.
pause

set /p MYSQL_USER="Enter MySQL username [root]: "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD="Enter MySQL password: "

echo.
echo Running migration...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% edu360_system < MIGRATION_ADD_ID_TO_JUNCTION_TABLES.sql

if errorlevel 1 (
    echo.
    echo ❌ Migration FAILED!
    echo.
    echo Có thể do:
    echo - Cột id đã tồn tại
    echo - MySQL credentials sai
    echo - Database không tồn tại
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Migration thành công!
echo.
echo Đã thêm cột id vào:
echo - session_lessons
echo - session_chapters
echo.
echo ========================================
echo BƯỚC TIẾP THEO
echo ========================================
echo.
echo 1. Restart backend (Ctrl+C rồi mvnw spring-boot:run)
echo 2. F5 lại trang ClassDetail
echo 3. Thử lưu nội dung buổi học lại
echo.
pause
