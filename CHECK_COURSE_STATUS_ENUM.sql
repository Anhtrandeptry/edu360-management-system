-- Kiểm tra cấu trúc của bảng courses
DESCRIBE courses;

-- Kiểm tra enum values hiện tại của column status
SHOW CREATE TABLE courses;

-- Nếu cần sửa, chạy câu lệnh sau:
-- ALTER TABLE courses MODIFY COLUMN status ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'ARCHIVED') NOT NULL;
