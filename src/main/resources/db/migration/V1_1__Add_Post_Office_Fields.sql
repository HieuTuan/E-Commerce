-- Migration script to add post office related fields
-- Add new fields to return_requests table for post office workflow

-- Add receipt confirmation fields
ALTER TABLE return_requests 
ADD COLUMN receipt_confirmed_at DATETIME NULL;

ALTER TABLE return_requests 
ADD COLUMN receipt_notes NVARCHAR(500) NULL;

ALTER TABLE return_requests 
ADD COLUMN receipt_confirmed_by BIGINT NULL;

-- Add foreign key constraint for receipt_confirmed_by
ALTER TABLE return_requests 
ADD CONSTRAINT FK_return_requests_receipt_confirmed_by 
FOREIGN KEY (receipt_confirmed_by) REFERENCES users(id);

-- Add assigned_post_office_id to users table
ALTER TABLE users 
ADD COLUMN assigned_post_office_id BIGINT NULL;

-- Add foreign key constraint for assigned_post_office_id
ALTER TABLE users 
ADD CONSTRAINT FK_users_assigned_post_office 
FOREIGN KEY (assigned_post_office_id) REFERENCES post_offices(id);

-- Update existing post office user to be assigned to first post office
UPDATE users 
SET assigned_post_office_id = (SELECT TOP 1 id FROM post_offices WHERE active = 1)
WHERE username = 'postoffice' AND assigned_post_office_id IS NULL;

-- Add index for better performance
CREATE INDEX IDX_return_requests_post_office_status ON return_requests(post_office_id, status);
CREATE INDEX IDX_return_requests_receipt_confirmed_at ON return_requests(receipt_confirmed_at);
CREATE INDEX IDX_users_assigned_post_office ON users(assigned_post_office_id);