# 🛒 E-Commerce Full — Hệ thống thương mại điện tử

> **Mô tả ngắn:** Hệ thống E-Commerce quản lý toàn diện từ sản phẩm, giỏ hàng, đặt hàng đến thanh toán và xử lý hoàn tiền/khiếu nại. Ứng dụng xây dựng trên kiến trúc **N-Layer** (Presentation / Service / Repository) với **Spring Boot**, phục vụ cả luồng người dùng (User) và quản trị viên (Admin).

## 🛠️ Tech Stack
Dự án sử dụng các công nghệ chính sau:
* **Ngôn ngữ:** Java (Xem phiên bản chi tiết trong `pom.xml`)
* **Framework:** Spring Boot
* **Build Tool:** Maven
* **Database:** SQL sever management (Cấu hình trong `application.properties`)
* **Frontend:** JavaScript + thymeleaf 
* **IDE:** IntelliJ IDEA

## ⚙️ Cài đặt Database (Database Setup)
1.  Tạo database mới: ECommercePlatform .
2.  Cập nhật file `src/main/resources/application.properties` với thông tin của người dùng:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=ECommercePlatform;encrypt=false;trustServerCertificate=true;characterEncoding=UTF-8;useUnicode=true;sendStringParametersAsUnicode=true
spring.datasource.username=sa
spring.datasource.password=123456
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA/Hibernate Configuration - Optimized for performance
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect
``` 



### 1) Purchase Flow (Ordering) 🚀

### 1. TÓM TẮT QUY TRÌNH (Purchase Workflow Overview)

**Actor:** Buyer (Người mua)

* 🛒 **Đặt hàng & Giỏ hàng (Buyer)**
    * **Main Flow:** Chọn sản phẩm → Thêm vào giỏ (`Add to cart`) → Tiến hành thanh toán (`Checkout`) → Tạo đơn hàng.
    * **Logic:** Hệ thống tự động kiểm tra tồn kho (`InventoryService`) trước khi cho phép tạo đơn.
    * **Dữ liệu:** Tạo bản ghi trong bảng `cart_items` và chuyển sang `order_items` khi chốt đơn.

* 💳 **Thanh toán & Xử lý (System/Gateway)**
    * **Payment Process:** Gọi API cổng thanh toán (`PaymentGateway`) → Xác thực giao dịch (`Capture`).
    * **Trạng thái:** Đơn hàng chuyển từ `CREATED` sang `PAID` (Đã thanh toán) hoặc `PROCESSING` (Đang xử lý).
    * **Success:** Sau khi thanh toán thành công → Trừ tồn kho (`Product Stock`) → Xóa giỏ hàng cũ → Gửi email xác nhận.

CRUD table (Purchase Flow)

| Step | Method / Function | CRUD | Database Impact |
|---|---|---:|---|
| 🛒 Add to cart | `CartController.addToCart` → `CartService.addItem` | Create | `cart_items` (insert) |
| ✏️ Update qty | `CartController.updateItem` → `CartService.updateItem` | Update | `cart_items` (update quantity) |
| 🗑️ Remove item | `CartController.removeItem` → `CartService.removeItem` | Delete | `cart_items` (delete) |
| ✅ Checkout / Create Order | `OrderController.checkout` → `OrderService.createOrder` | Create | `orders`, `order_items` (insert) |
| 📦 Reserve stock | `OrderService.reserveStock` or `InventoryService.reserveStock` | Update | `products` (stock decrement / reserved) |
| 🧾 Persist order items | `OrderService.saveOrderItems` → `OrderItemRepository.saveAll` | Create | `order_items` (insert) |
| 💳 Payment capture | `PaymentController.processPayment` → `PaymentService.capture` | Create / Update | `payments` (insert transaction, record gateway_tx, status) |
| 🔁 Finalize order | `OrderService.completeOrder` | Update | `orders` (status → PAID / PROCESSING) |
| 🧹 Clear cart | `CartService.clearByUser` → `CartRepository.deleteByUserId` | Delete | `cart_items` (delete for user) |

Notes:
- Payment must persist actual settled amount and gateway transaction id in `payments`.
- Stock validation performed before finalization in `CartService` / `OrderService`.
- Wrap critical sequences (create order → reserve stock → capture payment → finalize) in DB transactions.

---



### 2) Refund Flow (Refund / Complaint) 

**Actor:** Customer → Staff → System (Ví điện tử)

* 📝 **Khách hàng gửi yêu cầu hoàn hàng**
    * Điều kiện: Đơn hàng phải ở trạng thái `DELIVERED` và còn trong thời hạn khiếu nại.
    * Khách hàng chọn đơn → Bấm **"Yêu cầu hoàn hàng"** → Chọn lý do (`ReturnReason`) → Upload video bằng chứng (`evidenceVideo`) → Gửi yêu cầu.
    * Hệ thống lưu file video vào thư mục `uploads/videos/return-evidence/{orderId}/` và tạo bản ghi `return_requests` với trạng thái `REFUND_REQUESTED`.
    * Email thông báo gửi tự động đến staff.

* 🛡️ **Staff xem xét & phê duyệt**
    * Staff truy cập `/staff/returns` → Xem video bằng chứng → Ra quyết định:
        * ✅ **APPROVE (Chấp nhận):** Hệ thống tạo đơn vận chuyển ngược trên GHN (`createReturnShippingOrder`), trạng thái chuyển sang `RETURN_APPROVED`. **Hệ thống tự động gửi email thông báo cho khách hàng** (nếu email thất bại vẫn lưu approve).
        * ❌ **REJECT (Từ chối):** Trạng thái → `REFUND_REJECTED`, đơn hàng quay về `DELIVERED`, gửi email lý do từ chối.

* 🚚 **Khách hàng gửi hàng về**
    * Khách nhận email chứa mã trả hàng (`returnCode`) → Gửi hàng qua GHN.
    * Trạng thái: `RETURN_APPROVED` → `RETURNING`.

* 📬 **Staff xác nhận đã nhận hàng**
    * Staff bấm **"Xác nhận nhận hàng"** → Trạng thái: `RETURNING` → `RETURN_RECEIVED`.

* 💰 **Hoàn tiền vào ví (Wallet)**
    * Staff vào trang xử lý hoàn tiền → Bấm **"Hoàn tiền"**.
    * Hệ thống tự động **credit toàn bộ số tiền đơn hàng vào ví điện tử (`Wallet`) của khách hàng** (`WalletService.credit()`).
    * Trạng thái → `REFUNDED`. Email xác nhận hoàn tiền được gửi.
    * Khách hàng có thể kiểm tra số dư ví tại `/customer/wallet`.

```
[Customer]  →  Gửi yêu cầu + video  →  [return_requests: REFUND_REQUESTED]
                                                   ↓
[Staff]     →  Xem xét, duyệt       →  [RETURN_APPROVED] + GHN order tạo
                                                   ↓
[Customer]  →  Gửi hàng qua GHN    →  [RETURNING]
                                                   ↓
[Staff]     →  Xác nhận nhận hàng  →  [RETURN_RECEIVED]
                                                   ↓
[Staff]     →  Xác nhận hoàn tiền  →  [REFUNDED] + Wallet += totalAmount
```

#### 📋 Bảng CRUD (Return & Wallet Refund Flow)

| Step | Method / Function | CRUD | Database Impact |
| :--- | :--- | :---: | :--- |
| 📝 **Tạo yêu cầu** | `CustomerReturnController` → `ReturnService.createReturnRequest` | **Create** | `return_requests` (insert), video lưu local |
| 🛡️ **Staff duyệt** | `StaffReturnController.approveReturnRequest` → `ReturnService.approveReturnRequest` | **Update** | `return_requests` (status → RETURN_APPROVED), `orders` |
| 🚚 **GHN tạo đơn** | `GHNReturnService.createReturnShippingOrder` | **Update** | `return_requests` (ghn_order_code, ghn_fee) |
| 📬 **Xác nhận nhận hàng** | `StaffReturnController.confirmReceipt` | **Update** | `return_requests` (status → RETURN_RECEIVED) |
| 💰 **Hoàn tiền vào ví** | `StaffReturnController.completeRefund` → `WalletService.credit` | **Update** | `wallets` (balance += amount), `return_requests` (status → REFUNDED) |
| 📧 **Gửi email** | `NotificationService.sendApprovalNotification` | **Read** | Không ảnh hưởng DB (chỉ gửi email) |

---

### 3) Product Review Flow (Luồng đánh giá sản phẩm) ⭐

**Actor:** Customer → System

* ✅ **Điều kiện đánh giá**
    * Chỉ customer đã **mua và nhận hàng thành công** (`DELIVERED`) mới được đánh giá.
    * Mỗi customer chỉ được đánh giá **1 lần** cho mỗi sản phẩm trong mỗi đơn hàng.
    * Hệ thống kiểm tra qua `ReviewRepository.existsByUserAndProduct`.

* 📝 **Gửi đánh giá**
    * Khách hàng vào trang chi tiết sản phẩm → Cuộn xuống phần **"Đánh giá"** → Chọn số sao (1-5) + viết nhận xét → Bấm gửi.
    * Controller: `ReviewController` → `ReviewService.createReview`.
    * Lưu bản ghi vào bảng `reviews` (user_id, product_id, rating, comment, created_at).

* 🔄 **Cập nhật rating sản phẩm**
    * Sau khi lưu review, hệ thống tự động tính lại `averageRating` của sản phẩm:
        `averageRating = AVG(rating) FROM reviews WHERE product_id = ?`
    * Cập nhật vào bảng `products` (average_rating, review_count).

* 👁️ **Hiển thị đánh giá**
    * Tất cả người dùng (kể cả chưa đăng nhập) có thể xem danh sách review trên trang sản phẩm.
    * Hiển thị: Avatar, tên, số sao, nội dung, ngày đánh giá.

```
[Customer đã mua] → Vào trang sản phẩm → Chọn sao + nhận xét → Gửi
                                                  ↓
                    [reviews: insert]  →  [products: update averageRating]
                                                  ↓
                              Hiển thị cho tất cả người dùng
```

#### 📋 Bảng CRUD (Review Flow)

| Step | Method / Function | CRUD | Database Impact |
| :--- | :--- | :---: | :--- |
| ✅ **Kiểm tra quyền** | `ReviewService.canReview(userId, productId)` | **Read** | `orders`, `reviews` (check eligibility) |
| 📝 **Tạo review** | `ReviewController.createReview` → `ReviewService.createReview` | **Create** | `reviews` (insert: rating, comment, user_id, product_id) |
| 🔄 **Cập nhật rating** | `ReviewService.updateProductRating` → `ProductRepository.save` | **Update** | `products` (average_rating, review_count) |
| 👁️ **Xem reviews** | `ReviewController.getProductReviews` → `ReviewRepository.findByProductId` | **Read** | `reviews` (select by product_id) |

---

### 4) AI Activity Flow (Luồng hoạt động AI) 🤖

Hệ thống tích hợp 2 tính năng AI riêng biệt:

#### 4a. AI Phân loại sản phẩm (`AIClassificationService`)

* **Trigger:** Khi Admin/Staff tạo hoặc cập nhật sản phẩm mới.
* **Cơ chế hoạt động:**
    1. Lấy `productName` + `description` của sản phẩm.
    2. **Bước 1 — Keyword Matching:** Hệ thống tra bảng `CATEGORY_KEYWORDS` (từ khóa được định nghĩa sẵn) để tìm danh mục phù hợp nhất (đếm số keyword khớp, chọn danh mục có nhiều match nhất).
    3. **Bước 2 — AI API (GROQ / LLaMA):** Nếu keyword không đủ tin cậy (hoặc bật chế độ AI), gọi API GROQ với model `llama-3.1-8b-instant` để phân loại.
        * Gửi prompt: *"Classify this product: Name: `{name}`, Description: `{desc}`"*
        * Model trả về tên danh mục.
    4. **Fallback:** Nếu API lỗi → dùng lại kết quả keyword matching.
* **Output:** Tên danh mục (ví dụ: `laptop_gaming`, `laptop_student`, `General`).

```
[Admin tạo SP]  →  AIClassificationService.classifyProduct()
                          ↓
              [Keyword Matching] → match tốt → trả về danh mục
                          ↓ (không đủ tốt)
              [GROQ API / LLaMA] → AI phân loại → trả về danh mục
                          ↓ (API lỗi)
              [Fallback keyword result]
```

#### 4b. AI Chatbot hỗ trợ khách hàng

* **Trigger:** Khách hàng nhập câu hỏi vào hộp chat trên trang web.
* **Cơ chế hoạt động:**
    1. **Phân tích intent:** Hệ thống nhận diện ý định người dùng (tìm sản phẩm, hỏi giá, hỏi chính sách, v.v.) thông qua keyword matching trên câu hỏi.
    2. **Tìm sản phẩm:** Nếu intent là tìm kiếm sản phẩm → query DB lấy danh sách sản phẩm phù hợp (theo tên, giá, danh mục).
    3. **Gọi GROQ API:** Gửi context (danh sách sản phẩm + lịch sử hội thoại + câu hỏi) lên GROQ API (`llama-3.1-8b-instant`) để tạo câu trả lời tự nhiên.
    4. **Trả response:** Chatbot hiển thị câu trả lời kèm danh sách sản phẩm gợi ý (nếu có).
* **Xử lý lọc giá:** Chatbot phân tích các cụm từ tiếng Việt như *"dưới X triệu"*, *"trên X triệu"*, *"khoảng X triệu"* để lọc sản phẩm theo khoảng giá.

```
[Khách nhập câu hỏi]
        ↓
[Phân tích intent + extract từ khóa/giá]
        ↓
[Query DB: sản phẩm phù hợp]  ←→  [products, categories]
        ↓
[Gọi GROQ API với context đầy đủ]
        ↓
[Hiển thị câu trả lời + gợi ý sản phẩm]
```

| Tính năng AI | Model | Trigger | Output |
| :--- | :--- | :--- | :--- |
| Phân loại sản phẩm | `llama-3.1-8b-instant` (GROQ) | Admin tạo/sửa sản phẩm | Tên danh mục |
| Chatbot hỗ trợ | `llama-3.1-8b-instant` (GROQ) | Khách hàng chat | Câu trả lời tự nhiên + gợi ý SP |

---

## 🔐 Tài khoản Demo (Test Accounts)

Dưới đây là danh sách các tài khoản được khởi tạo tự động để giảng viên/người dùng test các chức năng. Bạn có thể sử dụng **Username** hoặc **Email** để đăng nhập (tùy vào cấu hình hệ thống).

| Vai trò (Role) | Username | Email | Password | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `tuan01062004kt@gmail.com` | `Admin123@` | **Full quyền:** Quản trị hệ thống, user, sản phẩm. |
| **Staff** | `staff` | `staff@ecommerce.com` | `Admin123@` | **Nhân viên:** Quản lý đơn hàng, đổi trả. |
| **Customer** | `customer` | `tuannhse182788@fpt.edu.vn` | `Admin123@` | **Khách hàng:** Mua sắm, xem lịch sử đơn. |

> **Lưu ý:** Dữ liệu này được tự động tạo bởi `TestDataLoader` khi chạy ứng dụng lần đầu.

# Secrets & Sensitive Configuration 🔒

The following properties in `application.properties` are **sensitive secrets** (account credentials, API keys, tokens). They must never be committed to source control or exposed in logs/public places.

- `spring.mail.password` — giru awvr xkyg gydq 
- `vnpay.hash-secret` — ZIQU8IKE4YBRYZFX8QTLXPWVNK1S56VW 
- `ghn.token` — 78be1310-ffe5-11f0-a3d6-dac90fb956b5  
- `ghn.webhook-secret` — hgfdsfggfdssdvgfdxcfdd
- `cloudinary.api-key` — 523872985863389

Why this matters
- 🔐 These values grant access to external services and financial/payment functionality.  
- ⚠️ Leaked secrets can lead to account compromise, financial loss, or data breach.

Recommended practices
1. Use environment variables or a secrets manager instead of hard-coding:
   - Example in `application.properties`:
     ```
     spring.mail.password=${SPRING_MAIL_PASSWORD}
     vnpay.hash-secret=${VNPAY_HASH_SECRET}
     ghn.token=${GHN_TOKEN}
     ghn.webhook-secret=${GHN_WEBHOOK_SECRET}
     cloudinary.api-key=${CLOUDINARY_API_KEY}
     ```
2. Add local secrets files to `.gitignore` (do not commit). 
3. Use cloud/infra secret stores for production:
   - GitHub Actions Secrets / GitLab CI variables  
   - AWS Secrets Manager / Parameter Store  
   - Azure Key Vault  
   - HashiCorp Vault
4. Rotate secrets regularly and minimize scopes/permissions.
5. Avoid printing secrets in logs and enable auditing for secret access.

Quick example: GitHub Actions usage


