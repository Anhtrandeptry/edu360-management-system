-- Add note column to attendances table and LATE status support
-- This migration adds a note field for teachers to add comments during attendance marking
-- and adds LATE as a new attendance status option

-- Add note column
ALTER TABLE attendances 
ADD COLUMN note TEXT NULL AFTER status;

-- The LATE enum value will be handled by Hibernate when the application restarts
-- Make sure to update any existing code that validates AttendanceStatus values

