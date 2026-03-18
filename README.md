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

**Actor:** Buyer (Người mua) | System (Hệ thống)

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

### 2) Refund Flow (Refund / Complaint) 💸

* 📝 **Gửi Yêu cầu Hoàn tiền (Buyer)**
    * **Main Flow:** Người dùng chọn đơn hàng → Bấm "Request refund" → Nhập lý do/số tiền → Gửi yêu cầu.
    * **Logic:** Hệ thống (`RefundService`) kiểm tra điều kiện (thời gian khiếu nại, trạng thái đơn) trước khi ghi nhận.
    * **Trạng thái:** Đơn khiếu nại chuyển sang `PENDING` (Chờ xử lý) hoặc `PENDING_REVIEW`.

* 🛡️ **Xử lý & Phê duyệt (Admin/System)**
    * **Manual Review:** Staff xem xét yêu cầu (`AdminController`) → Ra quyết định **APPROVE** (Đồng ý) hoặc **REJECT** (Từ chối).
    * **Payment Processing:** Nếu được duyệt -> thì đơn hàng sẽ được tạo bên GHN với trạng thái là "Chờ khách hàng gửi hàng" và gửi Email cho khách hàng là từ chối hoặc chấp nhận.
    * **Staff GHN**: Nhân viên giao hàng nhanh nhận hàng và xác nhận trạng thái thành đã nhận hàng(Vì lý do bảo mật bên GHN lên không thể đổi được trạng thái).
    * **Data Update:**
        * Cập nhật trạng thái `RefundRepository` & `OrderRepository`.
    * **Staff hoàn tiền:** Staff sẽ hoàn tiền và cập nhập ảnh hoàn tiền update lên hệ thống, khách hàng có thể xem bằng chứng đó và status đổi thành đã hoàn tiền*
    * **Success:** Hệ thống gửi thông báo (`NotificationService`) cho người dùng kết quả xử lý.

#### 📋 Bảng phân tích CRUD (Refund Flow)

| Step | Method / Function | CRUD | Database Impact |
| :--- | :--- | :---: | :--- |
| 📝 **Request refund** | `RefundController.requestRefund` → `RefundService.createRequest` | **Create** | `refunds` (insert record: requested_amount, reason, user_id) |
| 🔎 **Validate request** | `RefundService.validateRequest` | **Read** | `orders` (status), `payments` (transaction details) |
| ⏱️ **Check eligibility** | `RefundService.checkEligibility` / `isRefundable` | **Read** | Business logic (time window, order status) |
| 🛑 **Admin review** | `AdminController.approve` / `reject` → `handleAdminDecision` | **Update** | `refunds` (status → APPROVED / REJECTED) |
| 💳 **Execute refund** | `PaymentService.refundTransaction` → `Gateway.refund` | **Update** | `payments` (refund status), `refunds` (gateway_tx, executed_at) |
| 🔄 **Mark order** | `OrderService.markRefunded` | **Update** | `orders` (status → REFUNDED) |


Important business rules implemented in service layer:
- Only refund if order/payment status is allowed (e.g., PAID, DELIVERED within allowed window) — `RefundService.isRefundable(order)`.
- Refund amount must match actual settled amount from `payments` (use `payments.settled_amount`).
- Validate existence and success of original payment transaction before calling gateway.
- Log both the refund request and gateway response in `refunds` (requested_amount, refunded_amount, gateway_tx, status, handled_by_admin, reason).
- Wrap create-request → approve → gateway call → finalize updates in a DB transaction for auditability and consistency.

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
