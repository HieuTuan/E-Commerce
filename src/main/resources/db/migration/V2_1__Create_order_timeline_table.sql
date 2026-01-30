-- Create order_timeline table for tracking order status changes
CREATE TABLE order_timeline (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL,
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_by NVARCHAR(100) NULL,
    notes NVARCHAR(500) NULL,
    
    CONSTRAINT FK_order_timeline_order_id 
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Create index for better query performance
CREATE INDEX IX_order_timeline_order_id ON order_timeline(order_id);
CREATE INDEX IX_order_timeline_updated_at ON order_timeline(updated_at DESC);

-- Add comments for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Timeline entries tracking order status changes', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'order_timeline';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Reference to the order', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'order_timeline', 
    @level2type = N'COLUMN', @level2name = N'order_id';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Order status at this timeline point', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'order_timeline', 
    @level2type = N'COLUMN', @level2name = N'status';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Timestamp when status was updated', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'order_timeline', 
    @level2type = N'COLUMN', @level2name = N'updated_at';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'User who updated the status', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'order_timeline', 
    @level2type = N'COLUMN', @level2name = N'updated_by';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Additional notes for the status change', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'order_timeline', 
    @level2type = N'COLUMN', @level2name = N'notes';