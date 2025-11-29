-- Migration: Add teacher_course_versions table to link base course with teacher's customized version
-- Allows validation logic to accept chapters/lessons from either the base course or mapped teacher version.

CREATE TABLE IF NOT EXISTS teacher_course_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    base_course_id BIGINT NOT NULL,
    teacher_course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    CONSTRAINT fk_tcv_base_course FOREIGN KEY (base_course_id) REFERENCES courses(id),
    CONSTRAINT fk_tcv_teacher_course FOREIGN KEY (teacher_course_id) REFERENCES courses(id),
    CONSTRAINT fk_tcv_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    CONSTRAINT uq_tcv UNIQUE (base_course_id, teacher_course_id, teacher_id)
);

CREATE INDEX idx_tcv_base_teacher ON teacher_course_versions (base_course_id, teacher_id);
CREATE INDEX idx_tcv_teacher_course ON teacher_course_versions (teacher_course_id, teacher_id);
