-- Kiểm tra các user có role TEACHER và teacher record của họ
SELECT 
    u.id AS user_id,
    u.username,
    u.full_name,
    u.email,
    t.id AS teacher_id,
    t.subject_id,
    s.name AS subject_name
FROM users u
INNER JOIN user_roles ur ON u.id = ur.user_id
INNER JOIN roles r ON ur.role_id = r.id
LEFT JOIN teachers t ON u.id = t.user_id
LEFT JOIN subjects s ON t.subject_id = s.id
WHERE r.name = 'ROLE_TEACHER'
ORDER BY u.id;

-- Kiểm tra user có ROLE_TEACHER nhưng CHƯA có teacher record
SELECT 
    u.id AS user_id,
    u.username,
    u.full_name,
    u.email,
    'MISSING TEACHER RECORD' AS status
FROM users u
INNER JOIN user_roles ur ON u.id = ur.user_id
INNER JOIN roles r ON ur.role_id = r.id
LEFT JOIN teachers t ON u.id = t.user_id
WHERE r.name = 'ROLE_TEACHER'
  AND t.id IS NULL;
