@echo off
chcp 65001 >nul
echo ========================================
echo  FIX: Foreign Key Constraints
echo ========================================
echo.
echo Lỗi: session_lessons reference đến 'sessions'
echo Phải sửa thành: 'class_sessions'
echo.
pause

set /p MYSQL_USER="Enter MySQL username [root]: "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD="Enter MySQL password: "

echo.
echo Đang sửa foreign key constraints...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% edu360_system < MIGRATION_FIX_FOREIGN_KEYS.sql

if errorlevel 1 (
    echo.
    echo ❌ Migration FAILED!
    echo.
    echo Kiểm tra:
    echo - MySQL credentials
    echo - Constraint names có thể khác
    echo - Bảng class_sessions có tồn tại không
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Foreign keys đã được sửa!
echo.
echo session_lessons.session_id → class_sessions.id
echo session_lessons.lesson_id → course_lessons.id
echo session_chapters.session_id → class_sessions.id
echo session_chapters.chapter_id → course_chapters.id
echo.
echo ========================================
echo KHÔNG CẦN RESTART BACKEND
echo ========================================
echo.
echo Chỉ cần F5 lại trang và thử lưu nội dung!
echo.
pause
