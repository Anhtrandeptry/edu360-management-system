-- Allow course_id to be nullable in classes table
-- MySQL syntax
ALTER TABLE classes MODIFY COLUMN course_id BIGINT NULL;

-- Ensure foreign key allows NULLs (no change needed for FK itself)
-- If there is a NOT NULL constraint enforced via CHECK or older schema tools, drop and recreate FK
-- Replace `fk_classes_course` with the actual constraint name if different
-- SHOW CREATE TABLE classes; -- Use this to find exact FK name
-- Example:
-- ALTER TABLE classes DROP FOREIGN KEY fk_classes_course;
-- ALTER TABLE classes ADD CONSTRAINT fk_classes_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL ON UPDATE CASCADE;

-- Optional: set existing rows with invalid 0 course_id to NULL
UPDATE classes SET course_id = NULL WHERE course_id = 0;