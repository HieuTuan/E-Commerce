-- sql
-- Explanation: Create parent tables first (Users, Products), then Cart/Order and their item tables,
-- then Payment, Refund, Complaint. Types map: String->NVARCHAR(255), IDs->BIGINT IDENTITY,
-- LocalDateTime->DATETIME2, Boolean->BIT. Clear PK and FK constraint names included.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- Users (parent)
CREATE TABLE dbo.Users (
                           Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Users PRIMARY KEY,
                           username NVARCHAR(255) NOT NULL,
                           email NVARCHAR(255) NULL,
                           password NVARCHAR(255) NULL,
                           full_name NVARCHAR(255) NULL,
                           created_at DATETIME2 NULL,
                           updated_at DATETIME2 NULL,
                           is_active BIT DEFAULT 1
);

-- Products (parent)
CREATE TABLE dbo.Products (
                              Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Products PRIMARY KEY,
                              sku NVARCHAR(255) NULL,
                              name NVARCHAR(255) NOT NULL,
                              description NVARCHAR(255) NULL,
                              price DECIMAL(18,2) NOT NULL DEFAULT 0,
                              stock INT NOT NULL DEFAULT 0,
                              created_at DATETIME2 NULL,
                              updated_at DATETIME2 NULL,
                              is_active BIT DEFAULT 1
);

-- Carts (each user can have a cart)
CREATE TABLE dbo.Carts (
                           Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Carts PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           created_at DATETIME2 NULL,
                           updated_at DATETIME2 NULL,
                           CONSTRAINT FK_Carts_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(Id)
);

-- CartItems
CREATE TABLE dbo.CartItems (
                               Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CartItems PRIMARY KEY,
                               cart_id BIGINT NOT NULL,
                               product_id BIGINT NOT NULL,
                               quantity INT NOT NULL DEFAULT 1,
                               price DECIMAL(18,2) NOT NULL DEFAULT 0,
                               created_at DATETIME2 NULL,
                               updated_at DATETIME2 NULL,
                               CONSTRAINT FK_CartItems_Carts FOREIGN KEY (cart_id) REFERENCES dbo.Carts(Id),
                               CONSTRAINT FK_CartItems_Products FOREIGN KEY (product_id) REFERENCES dbo.Products(Id)
);

-- Orders (parent)
CREATE TABLE dbo.Orders (
                            Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Orders PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            order_number NVARCHAR(255) NOT NULL,
                            status NVARCHAR(255) NULL,
                            total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                            created_at DATETIME2 NULL,
                            updated_at DATETIME2 NULL,
                            CONSTRAINT FK_Orders_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(Id)
);

-- OrderItems
CREATE TABLE dbo.OrderItems (
                                Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OrderItems PRIMARY KEY,
                                order_id BIGINT NOT NULL,
                                product_id BIGINT NOT NULL,
                                quantity INT NOT NULL DEFAULT 1,
                                unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
                                created_at DATETIME2 NULL,
                                updated_at DATETIME2 NULL,
                                CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (order_id) REFERENCES dbo.Orders(Id),
                                CONSTRAINT FK_OrderItems_Products FOREIGN KEY (product_id) REFERENCES dbo.Products(Id)
);

-- Payments
CREATE TABLE dbo.Payments (
                              Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Payments PRIMARY KEY,
                              order_id BIGINT NOT NULL,
                              payment_provider NVARCHAR(255) NULL,
                              provider_transaction_id NVARCHAR(255) NULL,
                              amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                              status NVARCHAR(255) NULL,
                              paid_at DATETIME2 NULL,
                              created_at DATETIME2 NULL,
                              CONSTRAINT FK_Payments_Orders FOREIGN KEY (order_id) REFERENCES dbo.Orders(Id)
);

-- Refunds
CREATE TABLE dbo.Refunds (
                             Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Refunds PRIMARY KEY,
                             payment_id BIGINT NULL,
                             order_id BIGINT NOT NULL,
                             refund_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                             reason NVARCHAR(255) NULL,
                             status NVARCHAR(255) NULL,
                             created_at DATETIME2 NULL,
                             processed_at DATETIME2 NULL,
                             CONSTRAINT FK_Refunds_Payments FOREIGN KEY (payment_id) REFERENCES dbo.Payments(Id),
                             CONSTRAINT FK_Refunds_Orders FOREIGN KEY (order_id) REFERENCES dbo.Orders(Id)
);

-- Complaints (customer-initiated issues; may reference order or refund)
CREATE TABLE dbo.Complaints (
                                Id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Complaints PRIMARY KEY,
                                order_id BIGINT NULL,
                                refund_id BIGINT NULL,
                                user_id BIGINT NOT NULL,
                                title NVARCHAR(255) NULL,
                                description NVARCHAR(255) NULL,
                                status NVARCHAR(255) NULL,
                                created_at DATETIME2 NULL,
                                resolved_at DATETIME2 NULL,
                                CONSTRAINT FK_Complaints_Orders FOREIGN KEY (order_id) REFERENCES dbo.Orders(Id),
                                CONSTRAINT FK_Complaints_Refunds FOREIGN KEY (refund_id) REFERENCES dbo.Refunds(Id),
                                CONSTRAINT FK_Complaints_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(Id)
);

-- Optional: Indexes for common lookups (simple examples)
CREATE INDEX IDX_Orders_OrderNumber ON dbo.Orders(order_number);
CREATE INDEX IDX_Payments_ProviderTx ON dbo.Payments(provider_transaction_id);

COMMIT TRANSACTION;
GO

-- Sample mock data inserts
INSERT INTO dbo.Users (username, email, password, full_name, created_at, is_active)
VALUES ('alice', 'alice@example.com', 'hashedpwd', 'Alice Nguyen', SYSUTCDATETIME(), 1),
       ('bob', 'bob@example.com', 'hashedpwd', 'Bob Tran', SYSUTCDATETIME(), 1);

INSERT INTO dbo.Products (sku, name, description, price, stock, created_at, is_active)
VALUES ('SKU-001', 'T-Shirt', 'Cotton T-Shirt', 19.99, 100, SYSUTCDATETIME(), 1),
       ('SKU-002', 'Mug', 'Ceramic mug', 9.50, 200, SYSUTCDATETIME(), 1);

-- Create a cart for Alice
INSERT INTO dbo.Carts (user_id, created_at) VALUES (1, SYSUTCDATETIME());
-- Add items to cart
INSERT INTO dbo.CartItems (cart_id, product_id, quantity, price, created_at)
VALUES (1, 1, 2, 19.99, SYSUTCDATETIME()),
       (1, 2, 1, 9.50, SYSUTCDATETIME());

-- Create an order for Alice
INSERT INTO dbo.Orders (user_id, order_number, status, total_amount, created_at)
VALUES (1, 'ORD-1001', 'CREATED', 49.48, SYSUTCDATETIME());

-- Order items
INSERT INTO dbo.OrderItems (order_id, product_id, quantity, unit_price, created_at)
VALUES (1, 1, 2, 19.99, SYSUTCDATETIME()),
       (1, 2, 1, 9.50, SYSUTCDATETIME());

-- Payment for the order
INSERT INTO dbo.Payments (order_id, payment_provider, provider_transaction_id, amount, status, paid_at, created_at)
VALUES (1, 'Stripe', 'tx_abc123', 49.48, 'COMPLETED', SYSUTCDATETIME(), SYSUTCDATETIME());

-- Create a refund example (partial)
INSERT INTO dbo.Refunds (payment_id, order_id, refund_amount, reason, status, created_at)
VALUES (1, 1, 9.50, 'Damaged item', 'PENDING', SYSUTCDATETIME());

-- Complaint example
INSERT INTO dbo.Complaints (order_id, refund_id, user_id, title, description, status, created_at)
VALUES (1, 1, 1, 'Damaged mug', 'Mug arrived cracked', 'OPEN', SYSUTCDATETIME());

GO
