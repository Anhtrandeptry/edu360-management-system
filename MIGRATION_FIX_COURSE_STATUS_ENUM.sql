-- FIX LỖI: Data truncated for column 'status'
-- Nguyên nhân: Column status trong database chưa có đủ các giá trị enum mới

-- BƯỚC 1: Xem cấu trúc hiện tại
SHOW CREATE TABLE courses;

-- BƯỚC 2: Cập nhật column status với đầy đủ enum values
ALTER TABLE courses 
MODIFY COLUMN status ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'ARCHIVED') NOT NULL DEFAULT 'PENDING';

-- BƯỚC 3: Kiểm tra lại
DESCRIBE courses;

-- Sau khi chạy xong, thử lại tạo khóa học
