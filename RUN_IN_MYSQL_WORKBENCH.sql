-- ========================================
-- RUN THIS IN MYSQL WORKBENCH / phpMyAdmin
-- ========================================
-- Drop junction tables and let Hibernate recreate them
-- with correct foreign key constraints

USE edu360_system;

-- Step 1: Backup data (optional)
CREATE TABLE IF NOT EXISTS session_lessons_backup_20251126 AS 
SELECT * FROM session_lessons;

CREATE TABLE IF NOT EXISTS session_chapters_backup_20251126 AS 
SELECT * FROM session_chapters;

SELECT 'Backup created' as status;

-- Step 2: Drop tables with wrong foreign keys
DROP TABLE IF EXISTS session_lessons;
DROP TABLE IF EXISTS session_chapters;

SELECT 'Tables dropped successfully!' as status;
SELECT 'Now RESTART your Spring Boot backend.' as next_step;
SELECT 'Hibernate will auto-create these tables with correct constraints.' as info;

-- Step 3: After restart, verify new tables
-- Run this query after backend restart:
-- SHOW CREATE TABLE session_lessons;
-- SHOW CREATE TABLE session_chapters;
