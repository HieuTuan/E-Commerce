-- Add delivery issue support to orders table
ALTER TABLE orders ADD COLUMN has_delivery_issue BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing delivery_issue_reports table structure if needed
-- (The table should already exist based on the entity)
-- Add index for better performance
CREATE INDEX IF NOT EXISTS idx_orders_has_delivery_issue ON orders(has_delivery_issue);
CREATE INDEX IF NOT EXISTS idx_delivery_issue_reports_order_id ON delivery_issue_reports(order_id);
CREATE INDEX IF NOT EXISTS idx_delivery_issue_reports_status ON delivery_issue_reports(status);