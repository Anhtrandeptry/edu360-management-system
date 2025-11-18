-- Update attendances.status enum to include LATE
ALTER TABLE attendances
  MODIFY COLUMN status ENUM('UNMARKED','PRESENT','ABSENT','LATE') NOT NULL;