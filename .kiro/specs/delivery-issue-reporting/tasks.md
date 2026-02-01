# Kế hoạch triển khai

- [x] 1. Thiết lập database schema và entities



  - Tạo entity DeliveryIssueReport với tất cả các trường bắt buộc
  - Thêm trường hasDeliveryIssue vào entity Order hiện tại
  - Tạo script migration database
  - Thiết lập mối quan hệ entities (Order -> DeliveryIssueReport)
  - _Requirements: 5.1, 5.2_

- [ ]* 1.1 Viết property test cho việc tạo báo cáo cập nhật trạng thái đơn hàng
  - **Property 1: Report creation updates order state**
  - **Validates: Requirements 1.2, 1.3**

- [ ] 2. Triển khai tầng service cốt lõi
- [x] 2.1 Tạo interface và implementation cho DeliveryIssueService


  - Triển khai method createReport với validation
  - Triển khai method getPendingReports
  - Triển khai method resolveReport và rejectReport
  - Thêm logic ngăn chặn báo cáo trùng lặp
  - _Requirements: 1.2, 1.3, 2.1, 2.3, 2.4_

- [ ]* 2.2 Viết property test cho việc lưu trữ dữ liệu báo cáo
  - **Property 6: Report data persistence**
  - **Validates: Requirements 5.1**

- [ ]* 2.3 Viết property test cho việc admin xử lý cập nhật
  - **Property 7: Admin processing updates**
  - **Validates: Requirements 5.2**

- [x] 2.4 Triển khai cập nhật OrderService


  - Thêm method để cập nhật flag hasDeliveryIssue
  - Thêm method để lấy đơn hàng có vấn đề giao hàng
  - Tích hợp với logic quản lý đơn hàng hiện tại
  - _Requirements: 1.3, 2.3, 3.1, 3.2_

- [ ]* 2.5 Viết property test cho việc admin giải quyết báo cáo
  - **Property 4: Admin report resolution**
  - **Validates: Requirements 2.3, 6.4**

- [ ]* 2.6 Viết property test cho việc admin từ chối báo cáo
  - **Property 5: Admin report rejection**
  - **Validates: Requirements 2.4**

- [x] 3. Tạo REST API endpoints


- [x] 3.1 Triển khai DeliveryIssueController

  - Tạo endpoint POST /api/delivery-issues/report
  - Tạo endpoint GET /api/delivery-issues/pending cho admin
  - Tạo endpoint PUT /api/delivery-issues/{id}/resolve
  - Tạo endpoint PUT /api/delivery-issues/{id}/reject
  - Thêm các DTO request/response phù hợp


  - _Requirements: 1.2, 2.1, 2.3, 2.4, 6.1_

- [x] 3.2 Thêm validation và xử lý lỗi

  - Validate quyền của khách hàng khi tạo báo cáo
  - Validate quyền admin khi xử lý báo cáo
  - Xử lý trường hợp báo cáo trùng lặp
  - Thêm response lỗi toàn diện
  - _Requirements: 1.1, 2.2, 6.2, 6.3, 6.5_

- [ ]* 3.3 Viết unit test cho controller endpoints
  - Test tất cả hành vi endpoint và trường hợp lỗi
  - Test validation request/response

  - Test kiểm tra authorization
  - _Requirements: 1.2, 2.1, 2.3, 2.4_

- [ ] 4. Triển khai UI components cho khách hàng
- [x] 4.1 Tạo modal báo cáo vấn đề giao hàng

  - Thêm nút "Báo cáo" cho đơn hàng DELIVERED
  - Tạo modal với dropdown (chỉ có option "Chưa nhận được hàng")
  - Triển khai logic submit form
  - Thêm feedback thành công/lỗi
  - _Requirements: 1.1, 1.2_

- [x] 4.2 Cập nhật hiển thị trạng thái đơn hàng cho khách hàng


  - Sửa đổi render trạng thái đơn hàng để hiển thị DELIVERED màu đỏ với text "Khách hàng report"
  - Vô hiệu hóa nút "Báo cáo" sau khi báo cáo
  - Hiển thị trạng thái báo cáo cho khách hàng
  - _Requirements: 1.4, 1.5, 7.1, 7.2, 7.3, 7.4_

- [ ]* 4.3 Viết property test cho chỉ báo trực quan vấn đề giao hàng
  - **Property 2: Delivery issue visual indicator**
  - **Validates: Requirements 1.4, 3.1**

- [ ]* 4.4 Viết property test cho quản lý trạng thái nút báo cáo
  - **Property 3: Report button state management**
  - **Validates: Requirements 1.5**

- [ ]* 4.5 Viết property test cho hiển thị trạng thái khách hàng
  - **Property 9: Customer status display**
  - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [x] 5. Triển khai UI components cho admin


- [x] 5.1 Tạo trang quản lý Delivery Issue Report

  - Tạo bảng với các cột: Order ID, Customer, Issue Type, Reported Date, Status, Actions
  - Thêm chức năng "Xem chi tiết" với popup
  - Triển khai filtering và sorting

  - _Requirements: 2.1, 6.1, 6.2_

- [x] 5.2 Triển khai quy trình xử lý admin

  - Tạo quy trình "Đã giải quyết" với xác nhận và ghi chú
  - Tạo quy trình "Từ chối" với yêu cầu nhập lý do

  - Thêm cập nhật trạng thái real-time
  - _Requirements: 2.3, 2.4, 6.3, 6.4, 6.5_

- [x] 5.3 Cập nhật hiển thị danh sách đơn hàng admin

  - Hiển thị trạng thái DELIVERED màu đỏ với "(Khách hàng report)" cho đơn hàng có vấn đề
  - Thêm link "Xem báo cáo vấn đề" trong cột actions
  - Cập nhật hiển thị trạng thái sau khi giải quyết
  - _Requirements: 3.1, 3.2, 3.4_

- [ ]* 5.4 Viết property test cho tính đầy đủ của view chi tiết admin
  - **Property 11: Admin detail view completeness**
  - **Validates: Requirements 2.2, 6.2**

- [ ]* 5.5 Viết property test cho hiển thị link action admin
  - **Property 12: Admin action link visibility**
  - **Validates: Requirements 3.4**

- [ ] 6. Triển khai hệ thống thông báo
- [x] 6.1 Tạo service thông báo email

  - Triển khai template email cho các trạng thái báo cáo khác nhau
  - Thêm logic gửi email cho thay đổi trạng thái
  - Xử lý lỗi gửi email một cách graceful
  - _Requirements: 2.5, 7.5_

- [ ]* 6.2 Viết property test cho việc gửi thông báo
  - **Property 10: Notification sending**
  - **Validates: Requirements 2.5, 7.5**

- [ ] 7. Triển khai timeline và tính năng audit
- [x] 7.1 Thêm tạo timeline entry

  - Tạo timeline entry cho việc tạo báo cáo
  - Tạo timeline entry cho thay đổi trạng thái
  - Tích hợp với hệ thống timeline đơn hàng hiện tại
  - _Requirements: 5.3_

- [ ]* 7.2 Viết property test cho tạo timeline entry
  - **Property 8: Timeline entry creation**
  - **Validates: Requirements 5.3**

- [x] 7.3 Thêm thống kê và báo cáo


  - Tạo dashboard admin với thống kê vấn đề giao hàng
  - Hiển thị số lượng báo cáo theo trạng thái và thời gian
  - Thêm khả năng filtering và export
  - _Requirements: 5.5_




- [ ] 9. Integration testing và validation cuối cùng
- [x] 9.1 Tạo kịch bản test end-to-end


  - Test quy trình báo cáo hoàn chỉnh của khách hàng
  - Test quy trình giải quyết hoàn chỉnh của admin
  - Test các kịch bản lỗi và edge case
  - _Requirements: All_

- [ ]* 9.2 Viết integration test
  - Test API endpoint với database thật
  - Test UI component với tích hợp backend
  - Test tích hợp thông báo email
  - _Requirements: All_


- [x] 10. Checkpoint cuối cùng - Đảm bảo tất cả test đều pass


  - Đảm bảo tất cả test đều pass, hỏi user nếu có vấn đề phát sinh.