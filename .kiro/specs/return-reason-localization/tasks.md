# Kế hoạch Triển khai

- [x] 1. Sửa lỗi hiển thị lý do hoàn trả


  - Cập nhật template để sử dụng `displayName` thay vì tên enum
  - Thay đổi `${returnRequest.reason}` thành `${returnRequest.reason.displayName}`
  - Thêm null safety với fallback text phù hợp
  - _Yêu cầu: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [ ]* 1.1 Viết property test cho tính nhất quán hiển thị lý do hoàn trả
  - **Property 1: Return reason display name consistency**
  - **Validates: Requirements 1.1**

- [x] 2. Sửa lỗi mapping trạng thái hoàn trả


  - Cập nhật tên enum trong template switch statements
  - Thay đổi từ `PENDING`, `APPROVED`, `COMPLETED` sang `REFUND_REQUESTED`, `RETURN_APPROVED`, `REFUNDED`
  - Cập nhật tất cả các case trong th:switch để khớp với enum thực tế
  - Thêm mapping cho `RETURNING`, `RETURN_RECEIVED`, `REFUND_REJECTED`
  - _Yêu cầu: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [ ]* 2.1 Viết property test cho mapping trạng thái chính xác
  - **Property 2: Return status mapping and display consistency**
  - **Validates: Requirements 2.1**

- [x] 3. Cải thiện hiển thị trạng thái với displayName


  - Thêm option sử dụng `${returnRequest.status.displayName}` cho hiển thị đơn giản
  - Cập nhật badge colors cho các trạng thái mới
  - Đảm bảo tính nhất quán trong việc hiển thị trạng thái
  - _Yêu cầu: 3.1, 3.2, 3.5_

- [ ]* 3.1 Viết property test cho null safety
  - **Property 3: Null safety fallback behavior**
  - **Validates: Requirements 3.6**

- [x] 4. Kiểm tra và xác thực sửa chữa


  - Khởi động ứng dụng và truy cập trang chi tiết hoàn hàng
  - Xác minh lý do hoàn trả hiển thị bằng tiếng Việt
  - Xác minh trạng thái không còn hiển thị "Trạng thái không xác định"
  - Kiểm tra tất cả các trạng thái enum hiển thị đúng
  - Test với các return request có reason/status null
  - _Yêu cầu: 1.1, 2.1, 3.6_

- [ ]* 4.1 Viết unit test cho template rendering
  - Tạo test với dữ liệu return request giả lập
  - Kiểm tra hiển thị lý do hoàn trả với các enum values khác nhau
  - Test hiển thị trạng thái với tất cả ReturnStatus values
  - Test trường hợp reason/status null
  - _Yêu cầu: 1.1, 2.1, 3.6_

- [x] 5. Checkpoint - Đảm bảo tất cả tests pass



  - Chạy tất cả các tests để đảm bảo không có regression
  - Xác nhận template render không có lỗi
  - Kiểm tra performance của template với dữ liệu lớn