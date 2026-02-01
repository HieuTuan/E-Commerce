# Kế hoạch Triển khai

- [x] 1. Sửa lỗi ánh xạ trường trong template





  - Cập nhật file template `src/main/resources/templates/staff/returns/detail.html`
  - Thay đổi `${returnRequest.order.user.phoneNumber ?: 'Chưa có'}` thành `${returnRequest.order.user.phone ?: 'Chưa có'}`
  - Đảm bảo cú pháp Thymeleaf chính xác và toán tử Elvis hoạt động đúng
  - _Yêu cầu: 1.1, 1.4, 1.5_

- [ ]* 1.1 Viết property test cho tính nhất quán hiển thị số điện thoại
  - **Property 1: Phone number display consistency**
  - **Validates: Requirements 1.2**

- [ ]* 1.2 Viết property test cho việc render template không có exception
  - **Property 2: Template rendering without exceptions**  
  - **Validates: Requirements 1.4**

- [ ]* 1.3 Viết property test cho hành vi fallback của Elvis operator
  - **Property 3: Elvis operator fallback behavior**
  - **Validates: Requirements 2.4**

- [ ] 2. Kiểm tra và xác thực sửa chữa








  - Khởi động ứng dụng và truy cập trang chi tiết hoàn hàng
  - Xác minh không có lỗi SpelEvaluationException
  - Kiểm tra số điện thoại hiển thị chính xác cho người dùng có số điện thoại
  - Kiểm tra thông báo mặc định hiển thị cho người dùng không có số điện thoại
  - _Yêu cầu: 1.1, 1.2, 1.3_

- [ ]* 2.1 Viết unit test cho template rendering
  - Tạo test với dữ liệu return request giả lập
  - Kiểm tra hiển thị số điện thoại với các giá trị khác nhau
  - Test trường hợp phone field null/empty
  - _Yêu cầu: 1.2, 1.3_




- [x] 4. Kiểm tra toàn diện các template khác



  - Tìm kiếm các template khác có thể có lỗi ánh xạ trường tương tự
  - Kiểm tra các tham chiếu đến `phoneNumber` trong toàn bộ codebase
  - Sửa chữa bất kỳ lỗi ánh xạ trường nào khác được tìm thấy
  - _Yêu cầu: 2.1, 2.3_

- [ ]* 4.1 Viết integration test cho trang chi tiết hoàn hàng
  - Test end-to-end rendering trang với database entities thực
  - Kiểm tra với các scenario dữ liệu người dùng khác nhau
  - _Yêu cầu: 1.1, 1.4_
