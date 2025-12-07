-- Migration: Add phone column to parents table
-- Date: 2025-12-08
-- Purpose: Allow checking if parent already exists by phone number
--          to support multiple students under one parent account

-- Step 1: Add phone column (skip if already exists - comment out if column exists)
-- ALTER TABLE parents 
-- ADD COLUMN phone VARCHAR(15) UNIQUE;

-- Step 2: Update existing parents with phone from user table
-- This populates phone data from users.phone_number
UPDATE parents p
INNER JOIN users u ON p.user_id = u.id
SET p.phone = u.phone_number
WHERE u.phone_number IS NOT NULL;

-- Step 4: Add index for faster lookup
CREATE INDEX idx_parents_phone ON parents(phone);

-- Step 5: Verify migration
SELECT 'Migration completed successfully' AS status;

-- Check results
SELECT p.id, p.user_id, p.phone, u.phone_number as user_phone, u.full_name
FROM parents p
INNER JOIN users u ON p.user_id = u.id;

SELECT COUNT(*) as total_parents, 
       COUNT(phone) as parents_with_phone,
       COUNT(*) - COUNT(phone) as parents_without_phone
FROM parents;
