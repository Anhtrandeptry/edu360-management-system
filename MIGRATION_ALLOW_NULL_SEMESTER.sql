-- ========================================================================
-- Migration: Allow NULL semester_id in classes table
-- Date: 2025-11-15
-- Reason: Support creating classes without semester, using startDate/endDate
-- ========================================================================

USE edu360_system;

-- Step 1: Check current structure
SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'edu360_system' 
  AND TABLE_NAME = 'classes' 
  AND COLUMN_NAME = 'semester_id';

-- Step 2: Drop unique constraint if exists
-- Find constraint name first
SELECT CONSTRAINT_NAME 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = 'edu360_system' 
  AND TABLE_NAME = 'classes' 
  AND CONSTRAINT_TYPE = 'UNIQUE';

-- Drop the unique constraint (name + subject_id + semester_id)
-- Replace 'constraint_name' with actual name from query above
SET @constraintName = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = 'edu360_system' 
      AND TABLE_NAME = 'classes' 
      AND CONSTRAINT_TYPE = 'UNIQUE'
    LIMIT 1
);

SET @sql = IF(@constraintName IS NOT NULL, 
    CONCAT('ALTER TABLE classes DROP INDEX ', @constraintName), 
    'SELECT "No unique constraint found" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Allow NULL for semester_id column
ALTER TABLE classes MODIFY COLUMN semester_id BIGINT NULL;

-- Step 4: Verify the change
DESCRIBE classes;

SELECT 'Migration completed successfully!' AS status;
