-- Script kiểm tra session và lesson content
USE edu360_system;

-- 1. Kiểm tra cột lesson_content đã tồn tại chưa
SELECT 'Checking lesson_content column...' as status;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'edu360_system'
  AND TABLE_NAME = 'class_sessions'
  AND COLUMN_NAME = 'lesson_content';

-- 2. Xem các session hôm nay
SELECT 'Sessions today...' as status;
SELECT 
    cs.id,
    cs.class_id,
    c.name as class_name,
    cs.date,
    cs.status,
    cs.lesson_content,
    CHAR_LENGTH(cs.lesson_content) as content_length
FROM class_sessions cs
JOIN classes c ON c.id = cs.class_id
WHERE cs.date = CURDATE()
ORDER BY cs.id DESC
LIMIT 10;

-- 3. Xem session_chapters và session_lessons
SELECT 'Session chapters today...' as status;
SELECT 
    sc.id,
    sc.session_id,
    ch.title as chapter_title
FROM session_chapters sc
JOIN course_chapters ch ON ch.id = sc.chapter_id
JOIN class_sessions cs ON cs.id = sc.session_id
WHERE cs.date = CURDATE()
LIMIT 10;

SELECT 'Session lessons today...' as status;
SELECT 
    sl.id,
    sl.session_id,
    l.title as lesson_title
FROM session_lessons sl
JOIN course_lessons l ON l.id = sl.lesson_id
JOIN class_sessions cs ON cs.id = sl.session_id
WHERE cs.date = CURDATE()
LIMIT 10;

-- 4. Tất cả session có lesson_content
SELECT 'All sessions with content...' as status;
SELECT 
    cs.id,
    c.name as class_name,
    cs.date,
    LEFT(cs.lesson_content, 100) as content_preview
FROM class_sessions cs
JOIN classes c ON c.id = cs.class_id
WHERE cs.lesson_content IS NOT NULL
ORDER BY cs.date DESC, cs.id DESC
LIMIT 5;
