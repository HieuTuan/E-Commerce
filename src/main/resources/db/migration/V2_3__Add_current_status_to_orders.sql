-- Add current_status column to orders table for timeline tracking
ALTER TABLE orders 
ADD current_status NVARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Add constraint to ensure valid status values
ALTER TABLE orders 
ADD CONSTRAINT CHK_orders_current_status 
    CHECK (current_status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'CANCELLED'));

-- Update existing orders to have current_status match their status
UPDATE orders 
SET current_status = status 
WHERE current_status = 'PENDING';

-- Create index for better query performance
CREATE INDEX IX_orders_current_status ON orders(current_status);

-- Add comment for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Current status of the order for timeline tracking', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'orders', 
    @level2type = N'COLUMN', @level2name = N'current_status';