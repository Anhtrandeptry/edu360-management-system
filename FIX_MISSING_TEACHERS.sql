-- Script để tạo teacher records cho các user có ROLE_TEACHER nhưng chưa có teacher record
-- LƯU Ý: Chạy CHECK_TEACHER_RECORDS.sql trước để xác định user nào bị thiếu
-- Sau đó chọn subject_id phù hợp cho từng teacher

-- Ví dụ: Tạo teacher record cho user_id = 2 với subject_id = 1 (Toán)
-- INSERT INTO teachers (user_id, subject_id) VALUES (2, 1);

-- Hoặc tạo hàng loạt với subject mặc định (VD: subject_id = 1)
INSERT INTO teachers (user_id, subject_id)
SELECT u.id, 1 -- Thay 1 bằng subject_id phù hợp
FROM users u
INNER JOIN user_roles ur ON u.id = ur.user_id
INNER JOIN roles r ON ur.role_id = r.id
LEFT JOIN teachers t ON u.id = t.user_id
WHERE r.name = 'ROLE_TEACHER'
  AND t.id IS NULL;

-- Danh sách môn học để tham khảo:
SELECT id, name FROM subjects ORDER BY id;
