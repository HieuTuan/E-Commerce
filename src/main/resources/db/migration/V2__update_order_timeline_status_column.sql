-- Update order_timeline status column to support longer enum values
ALTER TABLE order_timeline ALTER COLUMN status NVARCHAR(30) NOT NULL;