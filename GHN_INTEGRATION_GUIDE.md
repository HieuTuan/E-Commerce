# Hướng dẫn GHN Integration - HOÀN THÀNH

## Trạng thái hiện tại

✅ **Migration thành công**: Đã xóa hoàn toàn PostOffice system và tích hợp GHN
✅ **Application khởi động**: Spring Boot chạy thành công trên port 8080
✅ **GHN Integration enabled**: GHNService đã được kích hoạt
✅ **Configuration consolidated**: Đã gộp và xóa file config trùng lặp

## Cấu hình GHN API Credentials

Cập nhật environment variables hoặc `application.properties` với thông tin thực:

```bash
# Environment Variables (Recommended)
export GHN_TOKEN=your-real-ghn-token
export GHN_SHOP_ID=your-real-shop-id
export GHN_WEBHOOK_SECRET=your-webhook-secret

# Optional warehouse configuration
export GHN_WAREHOUSE_DISTRICT_ID=1442
export GHN_WAREHOUSE_WARD_CODE=21211
export GHN_WAREHOUSE_ADDRESS="123 Main Street, District 1, Ho Chi Minh City"
export GHN_WAREHOUSE_NAME="Your Shop Warehouse"
export GHN_WAREHOUSE_PHONE="+84-123-456-789"
```

Hoặc cập nhật trực tiếp trong `application.properties`:

```properties
# GHN API Configuration
ghn.token=your-real-ghn-token
ghn.shop-id=your-real-shop-id
ghn.webhook-secret=your-webhook-secret
```

## Test GHN Integration

1. **Khởi động ứng dụng**:
   ```bash
   mvnw.cmd spring-boot:run
   ```

2. **Test webhook endpoint**:
   ```bash
   curl -X POST http://localhost:8080/api/ghn/webhook/status-update \
   -H "Content-Type: application/json" \
   -d '{
     "OrderCode": "TEST123",
     "Status": "delivered",
     "StatusText": "Đã giao hàng",
     "Time": "2024-01-31 10:00:00"
   }'
   ```

3. **Test return approval**: Tạo return request và approve để xem GHN order được tạo

## Cấu hình Webhook URL

Trong GHN dashboard, cấu hình webhook URL:
```
https://yourdomain.com/api/ghn/webhook/status-update
```

## Luồng hoạt động sau khi enable

### 1. Customer tạo return request
- Form không còn yêu cầu chọn PostOffice
- Chỉ cần thông tin sản phẩm và lý do

### 2. Staff approve return
- Tự động tạo GHN order
- Cập nhật return status thành RETURNING
- Gửi email với tracking info

### 3. GHN xử lý vận chuyển
- Webhook tự động cập nhật status
- Email thông báo cho customer
- Real-time tracking

### 4. Hoàn thành
- Status tự động chuyển thành RETURN_RECEIVED
- Staff xử lý refund

## Troubleshooting

### Lỗi GHN API
- Kiểm tra token và shop ID
- Xem log để debug API response
- Fallback về manual processing

### Webhook không hoạt động
- Kiểm tra URL configuration
- Verify webhook secret
- Check firewall/network settings

### Database migration
- Chạy migration script nếu chưa:
  ```sql
  -- V1_2__Remove_PostOffice_System.sql
  ```

## Monitoring

### Logs cần theo dõi
- GHN API calls: `GHNServiceImpl`
- Webhook events: `GHNWebhookController`
- Return status updates: `ReturnServiceImpl`

### Metrics quan trọng
- GHN order creation success rate
- Webhook processing time
- Return completion time

## Rollback Plan

Nếu có vấn đề:
1. Comment lại GHN code
2. Restart application
3. Process returns manually
4. Fix issues và re-enable

## Kết luận

GHN integration đã sẵn sàng và chỉ cần:
1. ✅ Cấu hình API credentials
2. ✅ Uncomment GHN service code  
3. ✅ Test và deploy

Hệ thống sẽ hoạt động hoàn toàn tự động với GHN integration!