-- Migration to remove PostOffice system and prepare for GHN integration
-- V1.2 - Remove PostOffice System

-- Remove POST_OFFICE role assignments from users
UPDATE users SET role_id = (SELECT id FROM roles WHERE name = 'CUSTOMER') 
WHERE role_id = (SELECT id FROM roles WHERE name = 'POST_OFFICE');

-- Remove POST_OFFICE role
DELETE FROM roles WHERE name = 'POST_OFFICE';

-- Remove post office related fields from users table
ALTER TABLE users DROP COLUMN IF EXISTS assigned_post_office_id;

-- Remove post office related fields from return_requests table  
ALTER TABLE return_requests DROP COLUMN IF EXISTS post_office_id;
ALTER TABLE return_requests DROP COLUMN IF EXISTS receipt_confirmed_at;
ALTER TABLE return_requests DROP COLUMN IF EXISTS receipt_notes;
ALTER TABLE return_requests DROP COLUMN IF EXISTS receipt_photo_url;
ALTER TABLE return_requests DROP COLUMN IF EXISTS receipt_confirmed_by;

-- Add GHN integration fields to return_requests table
ALTER TABLE return_requests ADD COLUMN ghn_order_code VARCHAR(50);
ALTER TABLE return_requests ADD COLUMN ghn_tracking_number VARCHAR(50);
ALTER TABLE return_requests ADD COLUMN ghn_status VARCHAR(50);
ALTER TABLE return_requests ADD COLUMN ghn_fee INTEGER;
ALTER TABLE return_requests ADD COLUMN pickup_time DATETIME;
ALTER TABLE return_requests ADD COLUMN delivery_time DATETIME;

-- Add indexes for GHN fields
CREATE INDEX idx_return_requests_ghn_order_code ON return_requests(ghn_order_code);
CREATE INDEX idx_return_requests_ghn_status ON return_requests(ghn_status);

-- Drop post_offices table if exists
DROP TABLE IF EXISTS post_offices;

-- Add comment
INSERT INTO migration_log (version, description, executed_at) 
VALUES ('V1.2', 'Remove PostOffice system and add GHN integration fields', NOW())
ON DUPLICATE KEY UPDATE executed_at = NOW();