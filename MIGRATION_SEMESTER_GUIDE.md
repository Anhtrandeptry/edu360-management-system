# Migration Guide: Allow NULL semester_id

## Problem

Database constraint error: `Column 'semester_id' cannot be null`

The application has been updated to support creating classes without a semester (using startDate/endDate instead), but the database schema still requires semester_id to be NOT NULL.

## Solution

Run the migration script to allow NULL values for the semester_id column.

## Steps to Fix

### Option 1: Using the Batch File (Windows)

1. Open Command Prompt as Administrator
2. Navigate to the project directory:
   ```
   cd d:\New360EDU-Ver3\edu360-management-system
   ```
3. Run the migration script:
   ```
   RUN_MIGRATION_SEMESTER.bat
   ```

### Option 2: Manual MySQL Command

1. Open MySQL command line or MySQL Workbench
2. Connect to your database:
   ```sql
   mysql -u root -p
   ```
3. Run the migration file:
   ```sql
   source d:/New360EDU-Ver3/edu360-management-system/MIGRATION_ALLOW_NULL_SEMESTER.sql
   ```

### Option 3: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your database
3. Open the file `MIGRATION_ALLOW_NULL_SEMESTER.sql`
4. Execute all statements

## Verification

After running the migration, verify the change:

```sql
USE edu360_system;
DESCRIBE classes;
```

The `semester_id` column should show:

- Type: `bigint`
- Null: `YES`
- Key: (empty or MUL, not part of unique constraint)

## Restart Application

After successful migration:

1. Stop your Spring Boot application
2. Restart it
3. Try creating a class again

## Rollback (if needed)

If you need to revert this change:

```sql
USE edu360_system;
ALTER TABLE classes MODIFY COLUMN semester_id BIGINT NOT NULL;
```

**Warning**: This will fail if there are existing records with NULL semester_id.
