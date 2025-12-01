-- =============================================================================
-- RESET SCRIPT: Xóa tất cả lớp học và courses được clone (giữ lại course base)
-- =============================================================================

-- Bước 1: Xem trước dữ liệu sẽ bị xóa
-- Các course base (do admin tạo, không có owner_teacher_id)
SELECT 'COURSE BASE (GIỮ LẠI):' as info;
SELECT id, title, owner_teacher_id, created_by FROM courses WHERE owner_teacher_id IS NULL;

-- Các course clone (có owner_teacher_id - sẽ bị xóa)
SELECT 'COURSE CLONE (SẼ XÓA):' as info;
SELECT id, title, owner_teacher_id, created_by FROM courses WHERE owner_teacher_id IS NOT NULL;

-- Các lớp học (sẽ bị xóa)
SELECT 'CLASSES (SẼ XÓA):' as info;
SELECT id, name, course_id, teacher_id FROM classes;

-- =============================================================================
-- Bước 2: XÓA DỮ LIỆU (thực hiện theo thứ tự để tránh lỗi foreign key)
-- =============================================================================

-- 2.1: Xóa enrollment của học sinh
DELETE FROM class_enrollments;

-- 2.2: Xóa session chapters và session lessons (nội dung buổi học)
DELETE FROM session_lessons;
DELETE FROM session_chapters;

-- 2.3: Xóa các buổi học (class sessions)
DELETE FROM class_sessions;

-- 2.4: Xóa lịch học (class schedules)
DELETE FROM class_schedules;

-- 2.5: Xóa các lớp học
DELETE FROM classes;

-- 2.6: Xóa lessons của courses clone
DELETE FROM course_lessons WHERE chapter_id IN (
    SELECT id FROM course_chapters WHERE course_id IN (
        SELECT id FROM courses WHERE owner_teacher_id IS NOT NULL
    )
);

-- 2.7: Xóa chapters của courses clone
DELETE FROM course_chapters WHERE course_id IN (
    SELECT id FROM courses WHERE owner_teacher_id IS NOT NULL
);

-- 2.8: Xóa teacher_course_versions (nếu có)
DELETE FROM teacher_course_versions;

-- 2.9: Xóa courses clone (có owner_teacher_id)
DELETE FROM courses WHERE owner_teacher_id IS NOT NULL;

-- =============================================================================
-- Bước 3: Xác nhận kết quả
-- =============================================================================
SELECT 'KẾT QUẢ SAU KHI XÓA:' as info;
SELECT 'Số lớp còn lại:', COUNT(*) FROM classes;
SELECT 'Số course base còn lại:', COUNT(*) FROM courses WHERE owner_teacher_id IS NULL;
SELECT 'Số course clone còn lại:', COUNT(*) FROM courses WHERE owner_teacher_id IS NOT NULL;
