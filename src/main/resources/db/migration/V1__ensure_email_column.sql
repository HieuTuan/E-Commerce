-- Ensure email column exists in users table
-- This script will be executed if using Flyway, otherwise Hibernate will handle it

-- Check if email column exists, if not add it
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'email')
BEGIN
    ALTER TABLE users ADD email NVARCHAR(100) NULL;
END

-- Make email unique if not already
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
               WHERE CONSTRAINT_TYPE = 'UNIQUE' 
               AND TABLE_NAME = 'users' 
               AND CONSTRAINT_NAME LIKE '%email%')
BEGIN
    -- First, update any NULL emails to avoid unique constraint issues
    UPDATE users SET email = username + '@temp.local' WHERE email IS NULL;
    
    -- Add unique constraint
    ALTER TABLE users ADD CONSTRAINT UK_users_email UNIQUE (email);
END

-- Make email not null if it's currently nullable
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
           WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'email' AND IS_NULLABLE = 'YES')
BEGIN
    -- Update any remaining NULL emails
    UPDATE users SET email = username + '@temp.local' WHERE email IS NULL;
    
    -- Make column not null
    ALTER TABLE users ALTER COLUMN email NVARCHAR(100) NOT NULL;
END