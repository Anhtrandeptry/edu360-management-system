-- Clean up backup tables
-- Run this AFTER you confirmed everything works fine

USE edu360_system;

-- Drop all backup tables
DROP TABLE IF EXISTS session_lessons_backup_20251126;
DROP TABLE IF EXISTS session_chapters_backup_20251126;
DROP TABLE IF EXISTS session_lessons_backup;
DROP TABLE IF EXISTS session_chapters_backup;

SELECT 'Backup tables cleaned up!' as status;

-- Verify remaining tables
SHOW TABLES LIKE '%session%';
