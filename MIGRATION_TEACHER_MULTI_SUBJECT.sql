-- MIGRATION: Convert teachers.subject_id (single subject) to teacher_subjects join table for many-to-many
-- STEP 0: Backup existing data
--   CREATE TABLE backup_teacher AS SELECT * FROM teachers;
--   CREATE TABLE backup_subject AS SELECT * FROM subjects;
--
-- STEP 1: Create new join table
CREATE TABLE teacher_subjects (
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, subject_id),
    CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    CONSTRAINT fk_ts_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);
--
-- STEP 2: Migrate existing single subject references
-- (Assumes teachers.subject_id still exists.)
INSERT INTO teacher_subjects (teacher_id, subject_id)
SELECT id AS teacher_id, subject_id FROM teachers WHERE subject_id IS NOT NULL;
--
-- STEP 3: Drop foreign key & column (names may differ by RDBMS / auto-generated constraint names)
-- Find constraint name first (example for MySQL):
--   SHOW CREATE TABLE teachers;  -- then identify the FK on subject_id
-- Example (adjust constraint name):
--   ALTER TABLE teachers DROP FOREIGN KEY fk_teachers_subject_id;
--   ALTER TABLE teachers DROP COLUMN subject_id;
--
-- STEP 4: (Optional) Add indexes to improve filtering by subject
CREATE INDEX idx_teacher_subjects_subject ON teacher_subjects(subject_id);
CREATE INDEX idx_teacher_subjects_teacher ON teacher_subjects(teacher_id);
--
-- STEP 5: Verify counts
--   SELECT COUNT(*) FROM backup_teacher bt WHERE bt.subject_id IS NOT NULL;
--   SELECT COUNT(*) FROM teacher_subjects;
-- They should match unless there were NULL subject rows.
--
-- STEP 6: Update application properties / ORM schema generation to validate ("none" or "validate")
--         Ensure the JPA entities now reflect ManyToMany.
--
-- ROLLBACK STRATEGY:
--   1) Re-add subject_id column to teachers.
--   2) For each teacher, pick first subject in teacher_subjects and set teachers.subject_id.
--   3) Drop teacher_subjects.
--
-- NOTE: After migration, update any reporting SQL referencing teachers.subject_id to join teacher_subjects instead.
