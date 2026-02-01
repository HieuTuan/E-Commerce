# Tích hợp Giao Hàng Nhanh (GHN) - Yêu cầu

## Tổng quan

Thay thế hệ thống PostOfficeStaff bằng tích hợp API Giao Hàng Nhanh (GHN) để xử lý vận chuyển hoàn hàng.

## Yêu cầu chức năng

### 1. Xóa PostOfficeStaff System
- Xóa PostOfficeController và tất cả endpoints
- Xóa PostOfficeService và implementation
- Xóa tất cả templates postoffice/
- Xóa role POST_OFFICE khỏi database
- Xóa các fields liên quan đến post office trong User entity

### 2. Tích hợp GHN API
- Tạo GHNService để gọi API Giao Hàng Nhanh
- Implement các chức năng:
  - Tạo đơn hoàn hàng
  - Theo dõi trạng thái vận chuyển
  - Xác nhận nhận hàng
  - Hủy đơn hàng

### 3. Cập nhật luồng hoàn hàng
- Khi staff approve return → Tự động tạo đơn GHN
- Theo dõi trạng thái qua GHN webhook
- Cập nhật trạng thái return request tự động
- Thông báo khách hàng về tiến trình

### 4. Quản lý cấu hình GHN
- Lưu trữ API key, shop ID
- Cấu hình địa chỉ kho hàng
- Quản lý dịch vụ vận chuyển

## Luồng mới

```
Customer Request Return → Staff Approve → GHN API Create Order → 
GHN Pickup → GHN Delivery → Webhook Update Status → 
Staff Process Return → Complete
```

## API GHN cần sử dụng

1. **Create Order**: Tạo đơn hoàn hàng
2. **Get Order Info**: Lấy thông tin đơn hàng  
3. **Cancel Order**: Hủy đơn hàng
4. **Webhook**: Nhận cập nhật trạng thái

## Cấu hình môi trường

```properties
ghn.api.url=https://dev-online-gateway.ghn.vn/shiip/public-api
ghn.api.token=your-api-token
ghn.shop.id=your-shop-id
ghn.warehouse.district.id=1442
ghn.warehouse.ward.code=21211
```