-- Create delivery_confirmations table for tracking delivery confirmations
CREATE TABLE delivery_confirmations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confirmed_at DATETIME2 NULL,
    rejection_reason NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT FK_delivery_confirmations_order_id 
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    
    CONSTRAINT CHK_delivery_confirmations_status 
        CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED'))
);

-- Create index for better query performance
CREATE INDEX IX_delivery_confirmations_order_id ON delivery_confirmations(order_id);
CREATE INDEX IX_delivery_confirmations_status ON delivery_confirmations(status);
CREATE INDEX IX_delivery_confirmations_created_at ON delivery_confirmations(created_at DESC);

-- Add comments for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Delivery confirmations from customers', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'delivery_confirmations';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Reference to the order (unique)', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'delivery_confirmations', 
    @level2type = N'COLUMN', @level2name = N'order_id';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Confirmation status: PENDING, CONFIRMED, REJECTED', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'delivery_confirmations', 
    @level2type = N'COLUMN', @level2name = N'status';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Timestamp when confirmation was made', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'delivery_confirmations', 
    @level2type = N'COLUMN', @level2name = N'confirmed_at';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Reason for rejection (if rejected)', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'delivery_confirmations', 
    @level2type = N'COLUMN', @level2name = N'rejection_reason';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Timestamp when confirmation request was created', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'delivery_confirmations', 
    @level2type = N'COLUMN', @level2name = N'created_at';