-- Add id column to session_lessons and session_chapters tables
-- These are junction tables for many-to-many relationships

USE edu360_system;

-- Check current structure of session_lessons
DESC session_lessons;

-- Add id column if not exists
ALTER TABLE session_lessons 
ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- Check current structure of session_chapters  
DESC session_chapters;

-- Add id column if not exists
ALTER TABLE session_chapters
ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- Verify changes
SELECT 'session_lessons structure:' as info;
DESC session_lessons;

SELECT 'session_chapters structure:' as info;
DESC session_chapters;
