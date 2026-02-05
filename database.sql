USE [ECommercePlatform];
GO

INSERT INTO [dbo].[categories]
(
    created_date,
    updated_date,
    name,
    description,
    image_url
)
VALUES
-- Dell
(SYSDATETIME(), SYSDATETIME(),
 'Dell',
 N'Hãng laptop Mỹ nổi tiếng với độ bền cao, hiệu năng ổn định, phù hợp cho văn phòng và doanh nghiệp.',
 'https://example.com/images/dell.png'),

-- HP
(SYSDATETIME(), SYSDATETIME(),
 'HP',
 N'Hewlett-Packard (HP) cung cấp laptop đa dạng từ học tập, văn phòng đến gaming và đồ họa.',
 'https://example.com/images/hp.png'),

-- Lenovo
(SYSDATETIME(), SYSDATETIME(),
 'Lenovo',
 N'Thương hiệu laptop hàng đầu với dòng ThinkPad bền bỉ và IdeaPad phổ thông.',
 'https://example.com/images/lenovo.png'),

-- ASUS
(SYSDATETIME(), SYSDATETIME(),
 'ASUS',
 N'Hãng laptop Đài Loan nổi bật với thiết kế sáng tạo, ZenBook, VivoBook và ROG gaming.',
 'https://example.com/images/asus.png'),

-- Acer
(SYSDATETIME(), SYSDATETIME(),
 'Acer',
 N'Lựa chọn laptop giá tốt, đa dạng phân khúc từ học sinh, sinh viên đến gaming.',
 'https://example.com/images/acer.png'),

-- MSI
(SYSDATETIME(), SYSDATETIME(),
 'MSI',
 N'Thương hiệu laptop gaming và đồ họa cao cấp với hiệu năng mạnh mẽ.',
 'https://example.com/images/msi.png'),

-- Apple
(SYSDATETIME(), SYSDATETIME(),
 'Apple',
 N'MacBook với hệ điều hành macOS, thiết kế cao cấp và hiệu năng tối ưu.',
 'https://example.com/images/apple.png'),

-- LG
(SYSDATETIME(), SYSDATETIME(),
 'LG',
 N'Laptop LG Gram nổi tiếng với trọng lượng nhẹ và thời lượng pin dài.',
 'https://example.com/images/lg.png'),

-- Samsung
(SYSDATETIME(), SYSDATETIME(),
 'Samsung',
 N'Laptop thiết kế mỏng nhẹ, đồng bộ tốt với hệ sinh thái Samsung.',
 'https://example.com/images/samsung.png'),

-- Microsoft Surface
(SYSDATETIME(), SYSDATETIME(),
 'Microsoft Surface',
 N'Dòng laptop cao cấp của Microsoft, thiết kế tinh tế, tối ưu cho Windows.',
 'https://example.com/images/surface.png');
GO


USE [ECommercePlatform];
GO

INSERT INTO [dbo].[products]
(
    featured,
    price,
    stock_quantity,
    category_id,
    created_date,
    updated_date,
    ai_category,
    name,
    description
)
VALUES
-- Dell
(1, 25000000, 20, 1, SYSDATETIME(), SYSDATETIME(),
 'Laptop văn phòng',
 N'Dell Inspiron 15 3520',
 N'Laptop Dell Inspiron 15, Intel Core i5, 16GB RAM, phù hợp làm việc và học tập.'),

-- HP
(0, 23000000, 15, 2, SYSDATETIME(), SYSDATETIME(),
 'Laptop văn phòng',
 N'HP Pavilion 14',
 N'HP Pavilion 14 thiết kế mỏng nhẹ, hiệu năng ổn định cho công việc hằng ngày.'),

-- Lenovo
(1, 27000000, 10, 3, SYSDATETIME(), SYSDATETIME(),
 'Laptop doanh nhân',
 N'Lenovo ThinkPad E14 Gen 5',
 N'Dòng ThinkPad bền bỉ, bảo mật cao, tối ưu cho doanh nghiệp.'),

-- ASUS
(1, 32000000, 12, 4, SYSDATETIME(), SYSDATETIME(),
 'Laptop gaming',
 N'ASUS ROG Strix G15',
 N'Laptop gaming ASUS ROG, CPU Ryzen 7, GPU RTX, hiệu năng mạnh mẽ.'),

-- Acer
(0, 18000000, 25, 5, SYSDATETIME(), SYSDATETIME(),
 'Laptop học sinh sinh viên',
 N'Acer Aspire 5',
 N'Laptop Acer giá tốt, phù hợp sinh viên và người dùng phổ thông.'),

-- MSI
(1, 42000000, 8, 6, SYSDATETIME(), SYSDATETIME(),
 'Laptop gaming',
 N'MSI Katana 15',
 N'Laptop gaming MSI với card đồ họa RTX, tản nhiệt hiệu quả.'),

-- Apple
(1, 36000000, 14, 7, SYSDATETIME(), SYSDATETIME(),
 'Laptop cao cấp',
 N'MacBook Air M2',
 N'MacBook Air chip Apple M2, pin lâu, thiết kế mỏng nhẹ.'),

-- LG
(0, 34000000, 6, 8, SYSDATETIME(), SYSDATETIME(),
 'Laptop mỏng nhẹ',
 N'LG Gram 14',
 N'LG Gram siêu nhẹ, pin trâu, thích hợp cho người hay di chuyển.'),

-- Samsung
(0, 29000000, 9, 9, SYSDATETIME(), SYSDATETIME(),
 'Laptop mỏng nhẹ',
 N'Samsung Galaxy Book 3',
 N'Laptop Samsung đồng bộ tốt với hệ sinh thái Galaxy.'),

-- Microsoft Surface
(1, 38000000, 7, 10, SYSDATETIME(), SYSDATETIME(),
 'Laptop cao cấp',
 N'Surface Laptop 5',
 N'Surface Laptop thiết kế cao cấp, màn hình PixelSense sắc nét.');
GO




