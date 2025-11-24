-- Migration: Thay đổi image_url từ VARCHAR(255) sang LONGTEXT để lưu base64
-- Chạy script này trong MySQL Workbench hoặc command line

USE edu360;

-- Thay đổi kiểu dữ liệu của cột image_url (MySQL sử dụng snake_case)
ALTER TABLE news 
MODIFY COLUMN image_url LONGTEXT;

-- Kiểm tra kết quả
DESCRIBE news;

-- Hoàn tất!
SELECT 'Migration completed successfully!' AS status;
