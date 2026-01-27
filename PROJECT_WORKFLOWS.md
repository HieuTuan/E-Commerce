# 📋 Danh Sách Workflow Hoàn Chỉnh - Commerce Platform

## 🏗️ Tổng Quan Dự Án

**Tên dự án:** Commerce Platform Full  
**Công nghệ:** Spring Boot 3.4.1, Java 21, Thymeleaf, Spring Security, JPA/Hibernate  
**Database:** Microsoft SQL Server  
**Kiến trúc:** MVC Pattern với Spring Boot  

---

## 🔐 1. WORKFLOW XÁC THỰC & PHÂN QUYỀN (Authentication & Authorization)

### 1.1 Đăng Ký Tài Khoản (User Registration)
**Controller:** `AuthController`  
**Endpoints:**
- `GET /register` - Hiển thị form đăng ký
- `POST /register` - Xử lý đăng ký tài khoản

**Quy trình:**
1. User điền form đăng ký (email, password, thông tin cá nhân)
2. Hệ thống validate thông tin (email unique, password strength, tuổi ≥ 16)
3. Tạo tài khoản tạm thời (chưa active)
4. Gửi OTP qua email để xác thực
5. Chuyển hướng đến trang xác thực OTP

### 1.2 Xác Thực Email với OTP (Email Verification)
**Endpoints:**
- `GET /verify-email` - Hiển thị form nhập OTP
- `POST /verify-email` - Xác thực OTP
- `POST /api/resend-otp` - Gửi lại OTP
- `GET /api/otp-status` - Kiểm tra trạng thái OTP

**Quy trình:**
1. User nhập OTP từ email
2. Hệ thống validate OTP (đúng mã, chưa hết hạn)
3. Kích hoạt tài khoản và tự động đăng nhập
4. Chuyển hướng đến trang chủ

### 1.3 Đăng Nhập (User Login)
**Endpoints:**
- `GET /login` - Hiển thị form đăng nhập
- `POST /login` - Xử lý đăng nhập (Spring Security)

**Quy trình:**
1. User nhập email/password
2. Spring Security xác thực thông tin
3. Phân quyền theo role (ADMIN, STAFF, USER)
4. Chuyển hướng theo role:
   - ADMIN → `/admin/dashboard`
   - STAFF → `/staff/dashboard`  
   - USER → `/` (trang chủ)

### 1.4 Quên Mật Khẩu với OTP (Password Reset)
**Endpoints:**
- `GET /forgot-password-otp` - Form nhập email
- `POST /forgot-password-otp` - Gửi OTP reset password
- `GET /verify-reset-otp` - Form nhập OTP reset
- `POST /verify-reset-otp` - Xác thực OTP reset
- `GET /reset-password-otp` - Form đặt mật khẩu mới
- `POST /reset-password-otp` - Cập nhật mật khẩu mới

**Quy trình:**
1. User nhập email để reset password
2. Hệ thống gửi OTP qua email
3. User nhập OTP để xác thực
4. User đặt mật khẩu mới
5. Cập nhật password và đăng nhập tự động
---

## 🛍️ 2. WORKFLOW QUẢN LÝ SẢN PHẨM (Product Management)

### 2.1 Xem Danh Sách Sản Phẩm (Product Listing)
**Controller:** `ProductController`  
**Endpoints:**
- `GET /products` - Danh sách sản phẩm với tìm kiếm và lọc
- `GET /products/{id}` - Chi tiết sản phẩm

**Quy trình:**
1. Hiển thị tất cả sản phẩm hoặc theo category
2. Hỗ trợ tìm kiếm theo tên sản phẩm
3. Lọc theo danh mục (category)
4. Hiển thị thông tin: tên, giá, hình ảnh, số lượng tồn kho
5. Click vào sản phẩm để xem chi tiết

### 2.2 Quản Lý Sản Phẩm (Admin/Staff)
**Controller:** `AdminController`, `StaffController`  
**Endpoints:**
- `GET /admin/products` - Danh sách sản phẩm (Admin)
- `GET /staff/products` - Danh sách sản phẩm (Staff)
- `GET /admin/products/new` - Form tạo sản phẩm mới
- `GET /admin/products/edit/{id}` - Form chỉnh sửa sản phẩm
- `POST /admin/products/save` - Lưu sản phẩm (tạo mới/cập nhật)
- `POST /admin/products/delete/{id}` - Xóa sản phẩm

**Quy trình Tạo/Sửa Sản Phẩm:**
1. Admin/Staff điền form sản phẩm (tên, mô tả, giá, category)
2. Upload hình ảnh sản phẩm (multiple files)
3. Hệ thống tối ưu hóa hình ảnh (resize, compress)
4. Tạo các kích thước: thumbnail, medium, large, original
5. Lưu thông tin sản phẩm và đường dẫn hình ảnh
6. Cập nhật AI classification cho sản phẩm

### 2.3 Quản Lý Hình Ảnh Sản Phẩm
**Endpoints:**
- `POST /admin/products/{productId}/images/{imageId}/delete` - Xóa hình ảnh
- `POST /admin/products/{productId}/images/{imageId}/set-primary` - Đặt hình chính
- `GET /files/images/{filename}` - Serve hình ảnh

**Quy trình:**
1. Upload multiple images cho sản phẩm
2. Tự động tạo các kích thước khác nhau
3. Đặt hình ảnh chính (primary image)
4. Xóa hình ảnh không cần thiết
5. Serve hình ảnh với caching

---

## 🛒 3. WORKFLOW GIỎ HÀNG (Shopping Cart)

### 3.1 Quản Lý Giỏ Hàng
**Controller:** `CartController`  
**Endpoints:**
- `GET /cart` - Xem giỏ hàng
- `POST /cart/add` - Thêm sản phẩm vào giỏ
- `POST /cart/update/{itemId}` - Cập nhật số lượng
- `POST /cart/remove/{itemId}` - Xóa sản phẩm khỏi giỏ

**Quy trình Thêm Vào Giỏ:**
1. User chọn sản phẩm và số lượng
2. Kiểm tra đăng nhập (redirect to login nếu chưa đăng nhập)
3. Validate số lượng tồn kho
4. Thêm vào giỏ hàng hoặc cập nhật số lượng nếu đã có
5. Hiển thị thông báo thành công với animation
6. Cập nhật số lượng giỏ hàng trên header

**Quy trình Cập Nhật Giỏ Hàng:**
1. User thay đổi số lượng sản phẩm
2. Validate số lượng (> 0 và <= stock)
3. Cập nhật database
4. Tính lại tổng tiền
5. Refresh trang giỏ hàng

---

## 💳 4. WORKFLOW THANH TOÁN (Checkout & Payment)

### 4.1 Quy Trình Checkout
**Controller:** `CheckoutController`  
**Endpoints:**
- `GET /checkout` - Trang thanh toán
- `POST /checkout/process` - Xử lý đơn hàng
- `GET /checkout/success` - Trang thành công

**Quy trình:**
1. Kiểm tra giỏ hàng không rỗng
2. User điền thông tin giao hàng:
   - Tên người nhận
   - Số điện thoại
   - Địa chỉ (Tỉnh/Thành phố → Quận/Huyện → Phường/Xã → Địa chỉ cụ thể)
3. Chọn phương thức thanh toán (VNPay/COD)
4. Xác nhận đơn hàng
5. Tạo Order và OrderItems
6. Xử lý thanh toán:
   - VNPay: Chuyển hướng đến cổng thanh toán
   - COD: Tạo đơn hàng trực tiếp
7. Xóa giỏ hàng sau khi đặt hàng thành công

### 4.2 Tích Hợp VNPay
**Controller:** `PaymentController`  
**Service:** `VNPayService`  
**Endpoints:**
- `GET /payment/vnpay/callback` - Callback từ VNPay

**Quy trình:**
1. Tạo URL thanh toán VNPay với thông tin đơn hàng
2. Chuyển hướng user đến VNPay
3. User thực hiện thanh toán trên VNPay
4. VNPay callback về hệ thống
5. Validate signature và thông tin thanh toán
6. Cập nhật trạng thái đơn hàng
7. Chuyển hướng đến trang thành công/thất bại