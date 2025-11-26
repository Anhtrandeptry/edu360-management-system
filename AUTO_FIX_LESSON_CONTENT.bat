@echo off
chcp 65001 >nul
echo ========================================
echo  KHẮC PHỤC LỖI 500 - Lesson Content
echo ========================================
echo.
echo JPA/Hibernate sẽ TỰ ĐỘNG tạo cột lesson_content
echo khi bạn restart backend!
echo.
echo ========================================
echo BƯỚC 1: Restart Backend
echo ========================================
echo.
echo 1. Đóng terminal Java đang chạy (Ctrl+C)
echo 2. Chạy lại backend:
echo.
echo    cd edu360-management-system
echo    mvnw spring-boot:run
echo.
echo 3. Chờ backend khởi động (xem log "Started Edu360...")
echo.
echo ========================================
echo BƯỚC 2: Kiểm tra cột đã được tạo
echo ========================================
echo.
pause

set /p MYSQL_USER="Enter MySQL username [root]: "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD="Enter MySQL password: "

echo.
echo Checking column...
echo.

mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% -D edu360_system -e "DESC class_sessions;" | findstr "lesson_content"

if errorlevel 1 (
    echo.
    echo ❌ Cột lesson_content CHƯA có!
    echo.
    echo Có thể do:
    echo - Backend chưa restart
    echo - JPA ddl-auto không phải 'update'
    echo - Lỗi kết nối DB
    echo.
    echo Hãy check log backend xem có lỗi gì!
    pause
    exit /b 1
) else (
    echo.
    echo ✅ Cột lesson_content ĐÃ CÓ trong database!
    echo.
)

echo ========================================
echo BƯỚC 3: Test chức năng
echo ========================================
echo.
echo 1. F5 lại trang ClassDetail
echo 2. ĐIỂM DANH trước (để tạo session)
echo 3. Chọn chương học
echo 4. Chọn bài học  
echo 5. Nhập nội dung
echo 6. Click "Lưu Nội Dung"
echo.
echo Nếu thành công, check database:
echo.
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% -D edu360_system -e "SELECT id, date, lesson_content FROM class_sessions WHERE lesson_content IS NOT NULL LIMIT 5;"
echo.
echo ✅ Hoàn tất!
pause
