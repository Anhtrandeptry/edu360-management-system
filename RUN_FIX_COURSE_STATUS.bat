@echo off
echo ============================================
echo FIX LOI: Data truncated for column status
echo ============================================
echo.
echo NGUYEN NHAN:
echo - Column status trong bang courses chua co du gia tri enum
echo - Enum hien tai chi co: PENDING, APPROVED (hoac khac)
echo - Can them: DRAFT, REJECTED, ARCHIVED
echo.
echo CACH FIX:
echo.
echo 1. Mo MySQL Workbench hoac MySQL Command Line
echo 2. Ket noi vao database: edu360_system
echo 3. Chay lenh sau:
echo.
echo    ALTER TABLE courses 
echo    MODIFY COLUMN status ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'ARCHIVED') 
echo    NOT NULL DEFAULT 'PENDING';
echo.
echo 4. Hoac chay file: MIGRATION_FIX_COURSE_STATUS_ENUM.sql
echo.
echo 5. Sau khi chay xong, thu lai tao khoa hoc
echo.
echo ============================================
echo.
echo Ban co muon mo MySQL va chay migration khong?
echo Luu y: Can co MySQL client trong PATH
echo.
pause
echo.
echo Dang thu ket noi MySQL...
mysql -u root -p edu360_system < MIGRATION_FIX_COURSE_STATUS_ENUM.sql
echo.
echo Neu thanh cong, hay thu lai tao khoa hoc!
pause
