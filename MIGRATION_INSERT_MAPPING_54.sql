-- Purpose: Insert missing mapping for teacher's personal course version #54
-- Base course: 49 (Toán 6 – Cơ bản)
-- Personal course (teacher customized): 54
-- Teacher: 19
-- Safeguards: Only insert if mapping does not already exist.

INSERT INTO teacher_course_versions (base_course_id, teacher_course_id, teacher_id)
SELECT 49, 54, 19
WHERE NOT EXISTS (
    SELECT 1 FROM teacher_course_versions
    WHERE base_course_id = 49
      AND teacher_course_id = 54
      AND teacher_id = 19
);

-- Verification query (run after executing this script):
-- SELECT * FROM teacher_course_versions WHERE base_course_id = 49 AND teacher_id = 19;

-- Expected result rows after insertion (at least these two if 55 already exists):
-- id | base_course_id | teacher_course_id | teacher_id
-- ?? | 49             | 54                | 19
--  1 | 49             | 55                | 19   (already existing)

-- If you need to rollback just this mapping (be careful):
-- DELETE FROM teacher_course_versions WHERE base_course_id = 49 AND teacher_course_id = 54 AND teacher_id = 19;
