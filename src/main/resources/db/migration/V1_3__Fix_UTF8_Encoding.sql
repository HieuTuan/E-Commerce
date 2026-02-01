-- Fix UTF-8 encoding for Vietnamese characters
-- Increase shipping_address column size and ensure proper collation

-- Drop and recreate orders table with proper UTF-8 support
ALTER TABLE orders ALTER COLUMN shipping_address NVARCHAR(500);
ALTER TABLE orders ALTER COLUMN customer_name NVARCHAR(200);
ALTER TABLE orders ALTER COLUMN notes NVARCHAR(500);

-- Update any existing corrupted data (optional - you may need to re-enter data)
-- This is just to ensure the column can handle Vietnamese characters properly