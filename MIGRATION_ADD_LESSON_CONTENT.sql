-- Add lesson_content column to class_sessions table
-- This column stores the text content that teacher writes for each session

ALTER TABLE class_sessions 
ADD COLUMN lesson_content TEXT NULL 
COMMENT 'Nội dung buổi học do giáo viên ghi chép';
