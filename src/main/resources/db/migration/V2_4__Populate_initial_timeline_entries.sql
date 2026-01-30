-- Populate initial timeline entries for existing orders
-- This creates a timeline entry for each existing order based on their current status

INSERT INTO order_timeline (order_id, status, updated_at, updated_by, notes)
SELECT 
    id as order_id,
    status,
    created_date as updated_at,
    'SYSTEM_MIGRATION' as updated_by,
    'Initial timeline entry created during migration' as notes
FROM orders
WHERE id NOT IN (
    SELECT DISTINCT order_id 
    FROM order_timeline 
    WHERE order_id IS NOT NULL
);

-- If orders have different created_date and updated_date, create additional entries
INSERT INTO order_timeline (order_id, status, updated_at, updated_by, notes)
SELECT 
    id as order_id,
    status,
    updated_date as updated_at,
    'SYSTEM_MIGRATION' as updated_by,
    'Status update entry created during migration' as notes
FROM orders
WHERE updated_date > created_date
AND id NOT IN (
    SELECT order_id 
    FROM order_timeline 
    WHERE updated_at = (SELECT updated_date FROM orders o WHERE o.id = order_timeline.order_id)
);

-- Create delivery confirmation requests for orders that are already delivered
INSERT INTO delivery_confirmations (order_id, status, created_at)
SELECT 
    id as order_id,
    'PENDING' as status,
    updated_date as created_at
FROM orders
WHERE status = 'DELIVERED'
AND id NOT IN (
    SELECT order_id 
    FROM delivery_confirmations 
    WHERE order_id IS NOT NULL
);