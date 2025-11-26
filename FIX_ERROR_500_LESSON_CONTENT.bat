@echo off
chcp 65001 >nul
echo ========================================
echo  HƯỚNG DẪN KHẮC PHỤC LỖI 500
echo ========================================
echo.
echo Lỗi: "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."
echo.
echo NGUYÊN NHÂN:
echo 1. Chưa chạy migration để thêm cột lesson_content
echo 2. Session chưa tồn tại (chưa điểm danh buổi học này)
echo.
echo ========================================
echo BƯỚC 1: Chạy Migration
echo ========================================
echo.
pause

set /p MYSQL_USER="Enter MySQL username [root]: "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD="Enter MySQL password: "

set MYSQL_DB=edu360_system

echo.
echo Đang chạy migration...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DB% < MIGRATION_ADD_LESSON_CONTENT.sql

if errorlevel 1 (
    echo.
    echo ❌ Migration thất bại!
    echo.
    echo Kiểm tra:
    echo - MySQL username/password
    echo - Database: edu360_system
    echo - File: MIGRATION_ADD_LESSON_CONTENT.sql
    pause
    exit /b 1
)

echo.
echo ✅ Migration thành công!
echo.
echo ========================================
echo BƯỚC 2: Kiểm tra Session
echo ========================================
echo.
echo Session phải tồn tại trước khi lưu nội dung.
echo.
echo Cách tạo session:
echo 1. Vào trang Điểm danh (ClassDetail)
echo 2. Chọn học sinh và điểm danh
echo 3. Sau đó mới lưu nội dung buổi học
echo.
echo ========================================
echo BƯỚC 3: Restart Backend
echo ========================================
echo.
echo Mở terminal mới và chạy:
echo   cd edu360-management-system
echo   mvnw spring-boot:run
echo.
echo Sau khi backend restart, F5 lại trang web và thử lại!
echo.
pause
