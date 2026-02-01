# Kế hoạch Triển khai

- [x] 1. Tạo controller và service cho nhân viên bưu cục



  - Tạo PostOfficeController với các endpoints cần thiết
  - Implement PostOfficeService với business logic
  - Thêm authentication và authorization cho POST_OFFICE_STAFF role
  - Tạo repository methods để filter returns theo post office
  - _Yêu cầu: 1.1, 4.1, 4.2, 4.3_

- [ ]* 1.1 Viết property test cho post office data filtering
  - **Property 1: Post office data filtering and access control**
  - **Validates: Requirements 1.1**

- [x] 2. Tạo giao diện dashboard cho nhân viên bưu cục


  - Tạo template postoffice/dashboard.html
  - Hiển thị thống kê returns (pending, completed today)
  - Tạo bảng danh sách returns gần đây
  - Implement navigation và layout cho post office interface
  - _Yêu cầu: 1.2, 1.3, 3.1, 3.2_

- [ ]* 2.1 Viết property test cho return information display
  - **Property 2: Return information display completeness**
  - **Validates: Requirements 1.2**


- [x] 3. Tạo trang chi tiết return cho nhân viên bưu cục

  - Tạo template postoffice/returns/detail.html
  - Hiển thị thông tin khách hàng và chi tiết return
  - Tạo form xác nhận nhận hàng với upload ảnh
  - Implement receipt confirmation functionality
  - _Yêu cầu: 2.1, 2.2, 2.3, 2.4, 3.3_

- [ ]* 3.1 Viết property test cho receipt confirmation workflow
  - **Property 3: Receipt confirmation workflow integrity**
  - **Validates: Requirements 2.1**



- [x] 4. Implement search và filter functionality

  - Tạo search form trong dashboard
  - Implement search logic trong service layer
  - Tạo template postoffice/returns/list.html cho search results
  - Thêm pagination cho danh sách returns
  - _Yêu cầu: 1.5_

- [ ]* 4.1 Viết property test cho search functionality
  - **Property 4: Search functionality accuracy**
  - **Validates: Requirements 1.5**

- [x] 5. Cập nhật database schema và entities

  - Thêm fields mới vào ReturnRequest entity (receiptConfirmedAt, receiptNotes, receiptPhotoUrl)
  - Tạo migration script để update database
  - Thêm POST_OFFICE_STAFF role vào UserRole enum
  - Update User entity để support post office assignment
  - _Yêu cầu: 2.2, 2.3, 2.4_

- [ ]* 5.1 Viết unit test cho entity updates
  - Test new fields trong ReturnRequest
  - Test POST_OFFICE_STAFF role functionality
  - Test database constraints và relationships
  - _Yêu cầu: 2.3, 2.4_

- [x] 6. Implement file upload cho receipt photos

  - Tạo file upload service cho receipt photos
  - Implement photo storage và retrieval
  - Thêm validation cho image files
  - Tạo thumbnail generation cho uploaded photos
  - _Yêu cầu: 2.2_

- [ ]* 6.1 Viết unit test cho file upload functionality
  - Test photo upload và storage
  - Test file validation
  - Test error handling cho upload failures
  - _Yêu cầu: 2.2_

- [x] 7. Tạo notification system cho workflow updates


  - Implement notification khi return được approve
  - Tạo notification cho store staff khi package được receive
  - Thêm email notifications (optional)
  - Update return timeline với status changes
  - _Yêu cầu: 2.5, 3.5_

- [ ]* 7.1 Viết unit test cho notification system
  - Test notification triggering
  - Test timeline updates
  - Test email sending (if implemented)
  - _Yêu cầu: 2.5_



- [x] 8. Kiểm tra và xác thực workflow hoàn chỉnh

  - Test complete workflow từ store approval đến post office receipt
  - Verify access control hoạt động đúng
  - Test search và filter functionality
  - Kiểm tra file upload và photo display
  - _Yêu cầu: 1.1, 2.1, 2.2, 4.1_

- [ ]* 8.1 Viết integration test cho complete workflow
  - Test end-to-end workflow
  - Test role-based access control


  - Test file upload integration
  - _Yêu cầu: 1.1, 2.1, 4.1_

- [x] 9. Checkpoint - Đảm bảo post office workflow hoạt động


  - Test với real data và user accounts
  - Verify notifications được gửi đúng
  - Confirm UI responsive và user-friendly
  - Check performance với large datasets