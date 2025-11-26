-- KIỂM TRA DỮ LIỆU TEACHER VÀ MÔN HỌC

-- 1. Xem tất cả teacher và môn học của họ
SELECT 
    t.id as teacher_id,
    u.id as user_id,
    u.username,
    u.full_name,
    s.id as main_subject_id,
    s.name as main_subject_name,
    GROUP_CONCAT(DISTINCT ts.id) as additional_subject_ids,
    GROUP_CONCAT(DISTINCT ts.name) as additional_subject_names
FROM teachers t
INNER JOIN users u ON t.user_id = u.id
INNER JOIN subjects s ON t.subject_id = s.id
LEFT JOIN teacher_subjects tss ON t.id = tss.teacher_id
LEFT JOIN subjects ts ON tss.subject_id = ts.id
GROUP BY t.id, u.id, u.username, u.full_name, s.id, s.name
ORDER BY t.id;

-- 2. Kiểm tra teacher cụ thể (thay {user_id} bằng user_id thật)
-- SELECT 
--     t.id as teacher_id,
--     t.user_id,
--     u.username,
--     s.id as main_subject_id,
--     s.name as main_subject_name
-- FROM teachers t
-- JOIN users u ON t.user_id = u.id
-- JOIN subjects s ON t.subject_id = s.id
-- WHERE t.user_id = {user_id};

-- 3. Kiểm tra môn phụ của teacher
-- SELECT 
--     t.id as teacher_id,
--     u.username,
--     ts.subject_id,
--     s.name as subject_name
-- FROM teachers t
-- JOIN users u ON t.user_id = u.id
-- LEFT JOIN teacher_subjects ts ON t.id = ts.teacher_id
-- LEFT JOIN subjects s ON ts.subject_id = s.id
-- WHERE t.user_id = {user_id};

-- 4. Danh sách tất cả môn học trong hệ thống
SELECT 
    id,
    name,
    status
FROM subjects
WHERE status = 'AVAILABLE'
ORDER BY name;
