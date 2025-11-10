-- Migration: Allow room_id to be NULL for online classes
-- Date: 2025-11-10
-- Description: Modify classes and class_sessions tables to allow room_id to be NULL to support online classes

-- Step 1: Modify classes table to allow NULL room_id
ALTER TABLE classes 
MODIFY COLUMN room_id BIGINT NULL 
COMMENT 'Foreign key to rooms table. NULL for online classes';

-- Step 2: Modify class_sessions table to allow NULL room_id
ALTER TABLE class_sessions 
MODIFY COLUMN room_id BIGINT NULL 
COMMENT 'Foreign key to rooms table. NULL for online class sessions';

-- Verify the changes
DESCRIBE classes;
DESCRIBE class_sessions;
