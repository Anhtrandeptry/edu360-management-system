-- Migration: Convert teacher profile from single table with JSON to multiple tables
-- This script:
-- 1. Creates new tables for certificates, experience, and education
-- 2. Removes JSON columns from teachers table
-- 3. Keeps years_of_experience, rating, and achievements columns

-- Step 1: Create new tables
CREATE TABLE IF NOT EXISTS teacher_certificates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    year INT,
    description TEXT,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    INDEX idx_teacher_cert (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher_experience (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    position VARCHAR(255),
    company VARCHAR(255),
    start_year INT,
    end_year INT,
    description TEXT,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    INDEX idx_teacher_exp (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher_education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    degree VARCHAR(255) NOT NULL,
    school VARCHAR(255),
    year INT,
    description TEXT,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    INDEX idx_teacher_edu (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Drop JSON columns from teachers table (if they exist)
SET @query_cert = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'teachers' 
     AND COLUMN_NAME = 'certificates') > 0,
    'ALTER TABLE teachers DROP COLUMN certificates',
    'SELECT "Column certificates does not exist"'
);
PREPARE stmt FROM @query_cert;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @query_exp = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'teachers' 
     AND COLUMN_NAME = 'experience') > 0,
    'ALTER TABLE teachers DROP COLUMN experience',
    'SELECT "Column experience does not exist"'
);
PREPARE stmt FROM @query_exp;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @query_edu = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'teachers' 
     AND COLUMN_NAME = 'education') > 0,
    'ALTER TABLE teachers DROP COLUMN education',
    'SELECT "Column education does not exist"'
);
PREPARE stmt FROM @query_edu;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Ensure the remaining columns exist (years_of_experience, rating, achievements)
-- These might have been added already, so we check first

SET @query_years = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'teachers' 
     AND COLUMN_NAME = 'years_of_experience') = 0,
    'ALTER TABLE teachers ADD COLUMN years_of_experience INT DEFAULT 0',
    'SELECT "Column years_of_experience already exists"'
);
PREPARE stmt FROM @query_years;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @query_rating = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'teachers' 
     AND COLUMN_NAME = 'rating') = 0,
    'ALTER TABLE teachers ADD COLUMN rating DOUBLE DEFAULT 0',
    'SELECT "Column rating already exists"'
);
PREPARE stmt FROM @query_rating;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @query_ach = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'teachers' 
     AND COLUMN_NAME = 'achievements') = 0,
    'ALTER TABLE teachers ADD COLUMN achievements TEXT',
    'SELECT "Column achievements already exists"'
);
PREPARE stmt FROM @query_ach;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Migration complete
SELECT 'Migration completed successfully' AS status;
