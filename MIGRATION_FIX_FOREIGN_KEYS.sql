-- Fix foreign key constraints for session_lessons and session_chapters
-- They are referencing wrong table name 'sessions' instead of 'class_sessions'

USE edu360_system;

-- Check current constraints
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'edu360_system'
  AND TABLE_NAME IN ('session_lessons', 'session_chapters')
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Drop old foreign keys from session_lessons
ALTER TABLE session_lessons DROP FOREIGN KEY IF EXISTS FKkk9xs6ddk2e6tm0ivxix7qg53;
ALTER TABLE session_lessons DROP FOREIGN KEY IF EXISTS FKb8qwvyb4gr35x0qd72edrqlqx;

-- Drop old foreign keys from session_chapters  
ALTER TABLE session_chapters DROP FOREIGN KEY IF EXISTS FK4w8qxxqr1fh8xjqvnb8qwvyb4;
ALTER TABLE session_chapters DROP FOREIGN KEY IF EXISTS FKa9c8xjqvnb8qwvyb4gr35x0q;

-- Add correct foreign keys for session_lessons
ALTER TABLE session_lessons
ADD CONSTRAINT fk_session_lessons_session
FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE;

ALTER TABLE session_lessons
ADD CONSTRAINT fk_session_lessons_lesson
FOREIGN KEY (lesson_id) REFERENCES course_lessons(id) ON DELETE CASCADE;

-- Add correct foreign keys for session_chapters
ALTER TABLE session_chapters
ADD CONSTRAINT fk_session_chapters_session
FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE;

ALTER TABLE session_chapters
ADD CONSTRAINT fk_session_chapters_chapter
FOREIGN KEY (chapter_id) REFERENCES course_chapters(id) ON DELETE CASCADE;

-- Verify new constraints
SELECT 'New constraints:' as info;
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'edu360_system'
  AND TABLE_NAME IN ('session_lessons', 'session_chapters')
  AND REFERENCED_TABLE_NAME IS NOT NULL;
