-- Drop and recreate junction tables to let Hibernate create correct foreign keys
-- This is safe because these are just link tables (can be recreated by teacher)

USE edu360_system;

-- Backup data first (optional)
CREATE TABLE IF NOT EXISTS session_lessons_backup AS SELECT * FROM session_lessons;
CREATE TABLE IF NOT EXISTS session_chapters_backup AS SELECT * FROM session_chapters;

-- Drop tables completely
DROP TABLE IF EXISTS session_lessons;
DROP TABLE IF EXISTS session_chapters;

-- Tables will be recreated by Hibernate on next startup with correct foreign keys
SELECT 'Tables dropped. Restart backend to recreate with correct constraints.' as status;
