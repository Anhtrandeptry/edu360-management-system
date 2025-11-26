-- Kiểm tra xem cột lesson_content đã tồn tại chưa
USE edu360_system;

SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'edu360_system'
  AND TABLE_NAME = 'class_sessions'
  AND COLUMN_NAME = 'lesson_content';

-- Nếu kết quả trống = chưa có cột, cần chạy migration
-- Nếu có 1 dòng = đã có cột rồi
