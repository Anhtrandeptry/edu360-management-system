-- ========================================================================
-- QUICK FIX: Run this in MySQL Workbench or phpMyAdmin
-- ========================================================================

-- Select your database
USE edu360_system;

-- Allow NULL for semester_id
ALTER TABLE classes MODIFY COLUMN semester_id BIGINT NULL;

-- Verify the change
DESCRIBE classes;

-- You should see semester_id with Null: YES

SELECT 'Migration completed! Please restart your Spring Boot application.' AS message;
