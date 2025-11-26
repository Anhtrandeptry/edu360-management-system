@echo off
chcp 65001 >nul
echo ========================================
echo  AUTO FIX: Let Hibernate recreate tables
echo ========================================
echo.
echo Sẽ DROP bảng session_lessons và session_chapters
echo Hibernate sẽ TỰ ĐỘNG tạo lại với constraint đúng!
echo.
echo Lưu ý: Dữ liệu trong 2 bảng này sẽ bị xóa
echo (chỉ là link giữa session và lesson/chapter)
echo.
pause

set /p MYSQL_USER="Enter MySQL username [root]: "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD="Enter MySQL password: "

echo.
echo Dropping tables...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% edu360_system < MIGRATION_DROP_JUNCTION_TABLES.sql

if errorlevel 1 (
    echo.
    echo ❌ Failed to drop tables!
    pause
    exit /b 1
)

echo.
echo ✅ Tables dropped successfully!
echo.
echo ========================================
echo RESTART BACKEND
echo ========================================
echo.
echo Hibernate sẽ tự động tạo lại:
echo - session_lessons (với FK đúng)
echo - session_chapters (với FK đúng)
echo.
echo Chạy lệnh:
echo   cd edu360-management-system
echo   mvnw spring-boot:run
echo.
echo Xem log sẽ thấy:
echo   Hibernate: create table session_lessons...
echo   Hibernate: create table session_chapters...
echo.
pause
