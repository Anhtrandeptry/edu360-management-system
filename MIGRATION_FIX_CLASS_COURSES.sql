-- Migration: Update existing classes to use cloned courses instead of base courses
-- This script creates cloned courses for classes that still have base courses

-- First, check which classes are using base courses (courses without ownerTeacher)
SELECT c.id as class_id, c.name as class_name, c.course_id, 
       co.title as course_title, co.owner_teacher_id
FROM classes c
JOIN courses co ON c.course_id = co.id
WHERE co.owner_teacher_id IS NULL;

-- For Class 2 (Dương Uyển Nhi - Văn), we need to update course_id to the teacher's course (id=7)
-- Check teacher's courses first
SELECT id, title, owner_teacher_id, subject_id FROM courses WHERE owner_teacher_id = 2;

-- If teacher already has a course for this class, update class to use it
-- UPDATE classes SET course_id = 7 WHERE id = 2;

-- Otherwise, you need to create new classes via admin panel to trigger the cloning logic
