-- Backfill mappings between base courses and teacher personal courses
-- Replace sample VALUES with your actual IDs (can be generated via SELECTs)

-- Example: base_course_id=49 maps to teacher_course_id=55 for teacher_id=19
INSERT INTO teacher_course_versions (base_course_id, teacher_course_id, teacher_id)
VALUES (49, 55, 19) AS new
ON DUPLICATE KEY UPDATE teacher_id = new.teacher_id;

-- Add more rows as needed below
-- INSERT INTO teacher_course_versions (base_course_id, teacher_course_id, teacher_id) VALUES (..., ..., ...) AS new ON DUPLICATE KEY UPDATE teacher_id = new.teacher_id;
