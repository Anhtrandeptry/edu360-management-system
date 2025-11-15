-- Migration: Drop obsolete 'code' column from classes table
-- Reason: Frontend no longer sends/uses class code; uniqueness handled by name + context.
USE edu360_system;

-- Check if column exists
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA='edu360_system' AND TABLE_NAME='classes' AND COLUMN_NAME='code';

-- If exists, drop any unique index referencing it
SET @uniq := (SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS 
  WHERE TABLE_SCHEMA='edu360_system' AND TABLE_NAME='classes' AND COLUMN_NAME='code' LIMIT 1);
SET @sql := IF(@uniq IS NOT NULL, CONCAT('ALTER TABLE classes DROP INDEX ', @uniq, ';'), 'SELECT "No unique index on code" AS info;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop the column
ALTER TABLE classes DROP COLUMN code;

-- Verify
DESCRIBE classes;

SELECT 'Dropped column code successfully' AS message;