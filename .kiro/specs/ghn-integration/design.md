# Thiết kế Tích hợp Giao Hàng Nhanh (GHN)

## Kiến trúc tổng quan

```
Return Request → Staff Approval → GHN Service → GHN API
                                      ↓
Webhook Handler ← GHN Status Updates ←
                                      ↓
Return Status Update → Email Notification → Customer
```

## Các thành phần mới

### 1. GHN Service Layer

```java
@Service
public interface GHNService {
    // Tạo đơn hoàn hàng
    GHNOrderResponse createReturnOrder(ReturnRequest returnRequest);
    
    // Theo dõi đơn hàng
    GHNOrderInfo getOrderInfo(String orderCode);
    
    // Hủy đơn hàng
    boolean cancelOrder(String orderCode);
    
    // Tính phí vận chuyển
    GHNFeeResponse calculateFee(GHNFeeRequest request);
}
```

### 2. GHN Configuration

```java
@ConfigurationProperties(prefix = "ghn")
@Data
public class GHNConfig {
    private String apiUrl;
    private String token;
    private Integer shopId;
    private Integer warehouseDistrictId;
    private String warehouseWardCode;
    private String warehouseAddress;
}
```

### 3. GHN DTOs

```java
// Request DTOs
public class GHNCreateOrderRequest {
    private Integer payment_type_id = 2; // Người gửi trả phí
    private String note;
    private String required_note = "KHONGCHOXEMHANG";
    private String return_phone;
    private String return_address;
    private String return_district_id;
    private String return_ward_code;
    private String client_order_code;
    private Integer to_district_id;
    private String to_ward_code;
    private String to_name;
    private String to_phone;
    private String to_address;
    private Integer cod_amount = 0;
    private String content;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer pick_station_id;
    private Integer deliver_station_id;
    private Integer insurance_value;
    private Integer service_id;
    private Integer service_type_id = 2;
    private String coupon;
    private List<GHNItem> items;
}

// Response DTOs
public class GHNOrderResponse {
    private Integer code;
    private String message;
    private GHNOrderData data;
}

public class GHNOrderData {
    private String order_code;
    private String sort_code;
    private String trans_type;
    private String ward_encode;
    private String district_encode;
    private GHNFee fee;
    private Integer total_fee;
    private String expected_delivery_time;
}
```

### 4. Webhook Handler

```java
@RestController
@RequestMapping("/api/ghn/webhook")
public class GHNWebhookController {
    
    @PostMapping("/status-update")
    public ResponseEntity<String> handleStatusUpdate(@RequestBody GHNWebhookPayload payload) {
        // Xử lý cập nhật trạng thái từ GHN
        // Cập nhật ReturnRequest status
        // Gửi thông báo cho khách hàng
        return ResponseEntity.ok("OK");
    }
}
```

## Cập nhật Entity

### ReturnRequest Entity

```java
@Entity
public class ReturnRequest {
    // Xóa các field liên quan post office
    // @ManyToOne
    // @JoinColumn(name = "post_office_id")
    // private PostOffice postOffice;
    
    // @Column(name = "receipt_confirmed_at")
    // private LocalDateTime receiptConfirmedAt;
    
    // Thêm fields cho GHN
    @Column(name = "ghn_order_code")
    private String ghnOrderCode;
    
    @Column(name = "ghn_tracking_number")
    private String ghnTrackingNumber;
    
    @Column(name = "ghn_status")
    private String ghnStatus;
    
    @Column(name = "ghn_fee")
    private Integer ghnFee;
    
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;
    
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;
}
```

## Luồng xử lý mới

### 1. Staff Approve Return

```java
@Service
public class ReturnServiceImpl {
    
    @Transactional
    public ReturnRequest approveReturn(Long returnId, String staffNotes) {
        ReturnRequest returnRequest = findById(returnId);
        returnRequest.setStatus(ReturnStatus.RETURN_APPROVED);
        returnRequest.setStaffNotes(staffNotes);
        returnRequest.setProcessedAt(LocalDateTime.now());
        
        // Tự động tạo đơn GHN
        try {
            GHNOrderResponse ghnResponse = ghnService.createReturnOrder(returnRequest);
            if (ghnResponse.getCode() == 200) {
                returnRequest.setGhnOrderCode(ghnResponse.getData().getOrder_code());
                returnRequest.setGhnTrackingNumber(ghnResponse.getData().getSort_code());
                returnRequest.setGhnFee(ghnResponse.getData().getTotal_fee());
                returnRequest.setStatus(ReturnStatus.RETURNING);
            }
        } catch (Exception e) {
            log.error("Failed to create GHN order for return {}", returnId, e);
            // Có thể fallback hoặc retry
        }
        
        return returnRequestRepository.save(returnRequest);
    }
}
```

### 2. GHN Status Mapping

```java
public enum GHNStatus {
    READY_TO_PICK("ready_to_pick", ReturnStatus.RETURNING),
    PICKING("picking", ReturnStatus.RETURNING), 
    PICKED("picked", ReturnStatus.RETURNING),
    STORING("storing", ReturnStatus.RETURNING),
    TRANSPORTING("transporting", ReturnStatus.RETURNING),
    SORTING("sorting", ReturnStatus.RETURNING),
    DELIVERING("delivering", ReturnStatus.RETURNING),
    DELIVERED("delivered", ReturnStatus.RETURN_RECEIVED),
    DELIVERY_FAIL("delivery_fail", ReturnStatus.RETURNING),
    WAITING_TO_RETURN("waiting_to_return", ReturnStatus.RETURNING),
    RETURN("return", ReturnStatus.RETURN_FAILED),
    RETURNED("returned", ReturnStatus.RETURN_FAILED),
    EXCEPTION("exception", ReturnStatus.RETURN_FAILED),
    DAMAGE("damage", ReturnStatus.RETURN_FAILED),
    LOST("lost", ReturnStatus.RETURN_FAILED);
    
    private final String ghnStatus;
    private final ReturnStatus returnStatus;
}
```

## Migration Plan

### Phase 1: Cleanup PostOffice System
1. Xóa PostOfficeController
2. Xóa PostOfficeService và implementation  
3. Xóa templates postoffice/
4. Tạo migration script xóa POST_OFFICE role
5. Xóa post office related fields

### Phase 2: Implement GHN Integration
1. Tạo GHN configuration
2. Implement GHNService
3. Tạo DTOs và response models
4. Implement webhook handler
5. Update ReturnRequest entity

### Phase 3: Update Return Workflow
1. Modify ReturnServiceImpl
2. Update return status flow
3. Add GHN order creation
4. Implement status synchronization
5. Update email notifications

### Phase 4: Testing & Deployment
1. Unit tests cho GHN service
2. Integration tests cho webhook
3. End-to-end testing
4. Production deployment

## Error Handling

```java
@Component
public class GHNErrorHandler {
    
    public void handleGHNError(GHNException e, ReturnRequest returnRequest) {
        switch (e.getErrorCode()) {
            case "INVALID_ADDRESS":
                // Thông báo khách hàng cập nhật địa chỉ
                break;
            case "INSUFFICIENT_BALANCE":
                // Thông báo admin nạp tiền
                break;
            case "SERVICE_UNAVAILABLE":
                // Retry sau hoặc fallback
                break;
            default:
                // Log error và thông báo staff
                break;
        }
    }
}
```

## Monitoring & Logging

```java
@Component
public class GHNMetrics {
    
    @EventListener
    public void onGHNOrderCreated(GHNOrderCreatedEvent event) {
        // Track success rate
        // Monitor response time
        // Log for audit
    }
    
    @EventListener  
    public void onGHNStatusUpdate(GHNStatusUpdateEvent event) {
        // Track delivery performance
        // Monitor status transition
    }
}
```