# Requirements Document

## Introduction

Hệ thống báo cáo vấn đề giao hàng cho phép khách hàng báo cáo khi chưa nhận được hàng, và admin có thể quản lý các báo cáo này để cập nhật trạng thái đơn hàng phù hợp.

## Glossary

- **Customer**: Khách hàng đã đặt hàng
- **Admin**: Quản trị viên hệ thống
- **Delivery_Issue_Report**: Báo cáo vấn đề giao hàng
- **Order_Status**: Trạng thái đơn hàng
- **Report_Status**: Trạng thái báo cáo (PENDING, RESOLVED, REJECTED)

## Requirements

### Requirement 1

**User Story:** Là một khách hàng, tôi muốn báo cáo khi chưa nhận được hàng, để admin có thể xử lý vấn đề giao hàng.

#### Acceptance Criteria

1. WHEN khách hàng ấn nút "Báo cáo" trên đơn hàng có trạng thái DELIVERED, THE System SHALL hiển thị modal với dropdown chỉ có lựa chọn "Chưa nhận được hàng"
2. WHEN khách hàng chọn "Chưa nhận được hàng" và submit, THE System SHALL tạo Delivery_Issue_Report với trạng thái PENDING
3. WHEN báo cáo được tạo thành công, THE System SHALL cập nhật trạng thái đơn hàng thành DELIVERED với flag "has_delivery_issue" = true
4. WHEN đơn hàng có delivery issue, THE System SHALL hiển thị trạng thái DELIVERED màu đỏ kèm text "Khách hàng report"
5. WHEN khách hàng đã báo cáo vấn đề, THE System SHALL vô hiệu hóa nút "Báo cáo" và hiển thị "Đã báo cáo vấn đề"

### Requirement 2

**User Story:** Là một admin, tôi muốn xem và xử lý các báo cáo vấn đề giao hàng, để có thể cập nhật trạng thái đơn hàng phù hợp.

#### Acceptance Criteria

1. WHEN admin truy cập trang "Delivery Issue Report", THE System SHALL hiển thị danh sách tất cả Delivery_Issue_Report với trạng thái PENDING
2. WHEN admin xem chi tiết một báo cáo, THE System SHALL hiển thị thông tin đơn hàng, khách hàng, và lý do báo cáo
3. WHEN admin chọn "Đã giải quyết" cho một báo cáo, THE System SHALL cập nhật Report_Status thành RESOLVED và Order_Status về DELIVERED bình thường (has_delivery_issue = false)
4. WHEN admin chọn "Từ chối" cho một báo cáo, THE System SHALL cập nhật Report_Status thành REJECTED và giữ nguyên Order_Status
5. WHEN báo cáo được giải quyết, THE System SHALL gửi thông báo cho khách hàng

### Requirement 3

**User Story:** Là một admin, tôi muốn thấy trạng thái đơn hàng được cập nhật real-time, để biết đơn hàng nào đang có vấn đề giao hàng.

#### Acceptance Criteria

1. WHEN đơn hàng có delivery issue, THE System SHALL hiển thị trạng thái "DELIVERED" màu đỏ kèm text "(Khách hàng report)" trong trang admin orders
2. WHEN admin giải quyết delivery issue, THE System SHALL cập nhật trạng thái về "DELIVERED" màu xanh bình thường và xóa text "(Khách hàng report)"
3. WHEN có delivery issue mới, THE System SHALL đồng bộ trạng thái trên tất cả các trang (customer, admin) ngay lập tức
4. WHEN admin xem Actions của đơn hàng có delivery issue, THE System SHALL hiển thị link "Xem báo cáo vấn đề"
5. WHEN delivery issue được giải quyết, THE System SHALL tự động cập nhật trạng thái trên tất cả các trang ngay lập tức

### Requirement 5

**User Story:** Là hệ thống, tôi muốn lưu trữ và theo dõi tất cả các báo cáo vấn đề giao hàng, để có thể phân tích và cải thiện dịch vụ.

#### Acceptance Criteria

1. WHEN tạo Delivery_Issue_Report, THE System SHALL lưu order_id, customer_email, issue_type, description, reported_at, status
2. WHEN admin xử lý báo cáo, THE System SHALL cập nhật resolved_at, resolved_by, admin_notes
3. WHEN có thay đổi trạng thái báo cáo, THE System SHALL tạo timeline entry cho đơn hàng
4. WHEN khách hàng xem đơn hàng có báo cáo, THE System SHALL hiển thị trạng thái báo cáo hiện tại
5. WHEN admin xem thống kê, THE System SHALL hiển thị số lượng báo cáo theo trạng thái và thời gian

### Requirement 6

**User Story:** Là một admin, tôi muốn có quy trình xử lý báo cáo rõ ràng, để đảm bảo tất cả vấn đề giao hàng được giải quyết đúng cách.

#### Acceptance Criteria

1. WHEN admin vào trang "Delivery Issue Report", THE System SHALL hiển thị bảng danh sách với các cột: Order ID, Customer, Issue Type, Reported Date, Status, Actions
2. WHEN admin click "Xem chi tiết" một báo cáo, THE System SHALL hiển thị popup với đầy đủ thông tin đơn hàng và lý do báo cáo
3. WHEN admin chọn "Đã giải quyết", THE System SHALL yêu cầu xác nhận và cho phép nhập ghi chú
4. WHEN admin xác nhận "Đã giải quyết", THE System SHALL cập nhật report status thành RESOLVED và order status về DELIVERED bình thường
5. WHEN admin chọn "Từ chối", THE System SHALL yêu cầu nhập lý do từ chối bắt buộc

### Requirement 7

**User Story:** Là một khách hàng, tôi muốn theo dõi trạng thái báo cáo vấn đề của mình, để biết admin đã xử lý chưa.

#### Acceptance Criteria

1. WHEN khách hàng xem đơn hàng đã báo cáo vấn đề, THE System SHALL hiển thị trạng thái báo cáo (Đang xử lý, Đã giải quyết, Từ chối)
2. WHEN báo cáo đang PENDING, THE System SHALL hiển thị "Đang xử lý - Chúng tôi sẽ liên hệ sớm nhất"
3. WHEN báo cáo được RESOLVED, THE System SHALL hiển thị "Đã giải quyết - Vấn đề đã được xử lý" và trạng thái đơn hàng về bình thường
4. WHEN báo cáo bị REJECTED, THE System SHALL hiển thị "Đã từ chối - Vui lòng liên hệ hotline để biết thêm chi tiết"
5. WHEN có cập nhật trạng thái báo cáo, THE System SHALL gửi email thông báo cho khách hàng