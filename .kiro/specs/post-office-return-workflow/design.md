# Design Document

## Overview

This design creates a complete workflow interface for post office staff to handle approved return requests. The solution includes a new post office dashboard, return processing interface, and integration with the existing return workflow to bridge the gap between store staff approval and package receipt confirmation.

## Architecture

The solution adds a new post office staff interface to the existing Spring Boot MVC architecture:

```
Store Staff Approval -> Return Status Update -> Post Office Interface
                                                        |
                                                        v
                                              Post Office Dashboard
                                                        |
                                                        v
                                        Return Processing & Receipt Confirmation
                                                        |
                                                        v
                                              Store Staff Notification
```

### New Components Required

1. **PostOfficeController**: Handle post office staff requests
2. **PostOfficeService**: Business logic for post office operations  
3. **Post Office Templates**: UI for post office staff
4. **Authentication**: Role-based access for post office staff

## Components and Interfaces

### New Controller
```java
@Controller
@RequestMapping("/postoffice")
public class PostOfficeController {
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth);
    
    @GetMapping("/returns")
    public String viewReturns(Model model, Authentication auth);
    
    @GetMapping("/returns/{id}")
    public String viewReturnDetail(@PathVariable Long id, Model model);
    
    @PostMapping("/returns/{id}/confirm-receipt")
    public String confirmReceipt(@PathVariable Long id, @RequestParam String notes, 
                                @RequestParam MultipartFile photo);
}
```

### New Service Methods
```java
@Service
public class PostOfficeService {
    
    List<ReturnRequest> getReturnsByPostOffice(Long postOfficeId);
    
    ReturnRequest confirmPackageReceipt(Long returnId, String notes, String photoUrl);
    
    List<ReturnRequest> searchReturns(Long postOfficeId, String searchTerm);
}
```

### New Templates Structure
```
src/main/resources/templates/postoffice/
├── dashboard.html          # Post office dashboard
├── returns/
│   ├── list.html          # List of returns to process
│   ├── detail.html        # Return detail view
│   └── confirm-receipt.html # Receipt confirmation form
└── layout/
    └── base.html          # Base layout for post office pages
```

## Data Models

### Enhanced ReturnRequest Entity
```java
// Add fields for post office processing
@Column(name = "receipt_confirmed_at")
private LocalDateTime receiptConfirmedAt;

@Column(name = "receipt_notes", columnDefinition = "NVARCHAR(500)")
private String receiptNotes;

@Column(name = "receipt_photo_url")
private String receiptPhotoUrl;

@ManyToOne
@JoinColumn(name = "receipt_confirmed_by")
private User receiptConfirmedBy; // Post office staff who confirmed
```

### Post Office Staff Role
```java
// Add new role for post office staff
public enum UserRole {
    CUSTOMER,
    STAFF,
    ADMIN,
    POST_OFFICE_STAFF  // New role
}
```

## User Interface Design

### Post Office Dashboard
```html
<!-- Dashboard showing key metrics and pending returns -->
<div class="container">
    <h2>Bảng Điều Khiển Bưu Cục</h2>
    
    <!-- Statistics Cards -->
    <div class="row">
        <div class="col-md-3">
            <div class="card bg-warning">
                <div class="card-body">
                    <h5>Chờ Nhận Hàng</h5>
                    <h3 th:text="${pendingReturns}">5</h3>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card bg-success">
                <div class="card-body">
                    <h5>Đã Nhận Hôm Nay</h5>
                    <h3 th:text="${todayReceived}">12</h3>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Recent Returns Table -->
    <div class="card mt-4">
        <div class="card-header">
            <h5>Đơn Hoàn Trả Gần Đây</h5>
        </div>
        <div class="card-body">
            <table class="table">
                <thead>
                    <tr>
                        <th>Mã Hoàn Trả</th>
                        <th>Khách Hàng</th>
                        <th>Trạng Thái</th>
                        <th>Ngày Duyệt</th>
                        <th>Thao Tác</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="return : ${recentReturns}">
                        <td th:text="${return.returnCode}">RET001</td>
                        <td th:text="${return.order.user.fullName}">Nguyễn Văn A</td>
                        <td>
                            <span class="badge bg-warning" th:text="${return.status.displayName}">
                                Chờ nhận hàng
                            </span>
                        </td>
                        <td th:text="${#temporals.format(return.updatedAt, 'dd/MM/yyyy')}">01/01/2024</td>
                        <td>
                            <a th:href="@{/postoffice/returns/{id}(id=${return.id})}" 
                               class="btn btn-sm btn-primary">Xem Chi Tiết</a>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
```

### Return Detail View for Post Office
```html
<!-- Detailed view with receipt confirmation -->
<div class="container">
    <h2>Chi Tiết Đơn Hoàn Trả</h2>
    
    <!-- Return Information -->
    <div class="row">
        <div class="col-md-8">
            <!-- Customer Info -->
            <div class="card mb-4">
                <div class="card-header">
                    <h5>Thông Tin Khách Hàng</h5>
                </div>
                <div class="card-body">
                    <p><strong>Tên:</strong> <span th:text="${returnRequest.order.user.fullName}">Nguyễn Văn A</span></p>
                    <p><strong>Điện thoại:</strong> <span th:text="${returnRequest.order.user.phone}">0123456789</span></p>
                    <p><strong>Email:</strong> <span th:text="${returnRequest.order.user.email}">customer@example.com</span></p>
                </div>
            </div>
            
            <!-- Return Details -->
            <div class="card mb-4">
                <div class="card-header">
                    <h5>Chi Tiết Hoàn Trả</h5>
                </div>
                <div class="card-body">
                    <p><strong>Mã hoàn trả:</strong> <code th:text="${returnRequest.returnCode}">RET001</code></p>
                    <p><strong>Lý do:</strong> <span th:text="${returnRequest.reason.displayName}">Sản phẩm bị lỗi</span></p>
                    <p><strong>Mô tả:</strong> <span th:text="${returnRequest.detailedDescription}">Chi tiết lỗi...</span></p>
                </div>
            </div>
        </div>
        
        <div class="col-md-4">
            <!-- Receipt Confirmation -->
            <div class="card" th:if="${returnRequest.status.name() == 'RETURNING'}">
                <div class="card-header">
                    <h5>Xác Nhận Nhận Hàng</h5>
                </div>
                <div class="card-body">
                    <form th:action="@{/postoffice/returns/{id}/confirm-receipt(id=${returnRequest.id})}" 
                          method="post" enctype="multipart/form-data">
                        
                        <div class="mb-3">
                            <label for="notes" class="form-label">Ghi chú về tình trạng hàng:</label>
                            <textarea class="form-control" id="notes" name="notes" rows="3" 
                                      placeholder="Mô tả tình trạng gói hàng khi nhận..."></textarea>
                        </div>
                        
                        <div class="mb-3">
                            <label for="photo" class="form-label">Chụp ảnh gói hàng:</label>
                            <input type="file" class="form-control" id="photo" name="photo" 
                                   accept="image/*" required>
                        </div>
                        
                        <button type="submit" class="btn btn-success w-100">
                            <i class="fas fa-check me-2"></i>Xác Nhận Đã Nhận Hàng
                        </button>
                    </form>
                </div>
            </div>
            
            <!-- Already Received -->
            <div class="card" th:if="${returnRequest.status.name() == 'RETURN_RECEIVED'}">
                <div class="card-header">
                    <h5>Đã Nhận Hàng</h5>
                </div>
                <div class="card-body">
                    <p><strong>Ngày nhận:</strong> <span th:text="${#temporals.format(returnRequest.receiptConfirmedAt, 'dd/MM/yyyy HH:mm')}">01/01/2024 10:00</span></p>
                    <p><strong>Nhân viên xác nhận:</strong> <span th:text="${returnRequest.receiptConfirmedBy.fullName}">Nhân viên A</span></p>
                    <p><strong>Ghi chú:</strong> <span th:text="${returnRequest.receiptNotes}">Hàng nguyên vẹn</span></p>
                    
                    <div th:if="${returnRequest.receiptPhotoUrl}">
                        <strong>Ảnh gói hàng:</strong>
                        <img th:src="${returnRequest.receiptPhotoUrl}" class="img-fluid mt-2" alt="Ảnh gói hàng">
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
```
## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, after eliminating redundancy, the following properties can be tested:

**Property 1: Post office data filtering and access control**
*For any* post office staff user, when accessing the returns interface, only returns assigned to their specific post office should be visible and actionable
**Validates: Requirements 1.1, 4.1, 4.2, 4.3, 4.4**

**Property 2: Return information display completeness**
*For any* return request visible to post office staff, when viewing return details, all required customer information, order details, and return specifics should be displayed correctly
**Validates: Requirements 1.2, 1.3, 3.1, 3.2, 3.3**

**Property 3: Receipt confirmation workflow integrity**
*For any* return in "RETURNING" status, when post office staff confirm package receipt with notes and photo, the return status should update to "RETURN_RECEIVED" and all confirmation data should be saved
**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

**Property 4: Search functionality accuracy**
*For any* search query (customer name, phone, or return code), when post office staff search for returns, only matching returns assigned to their post office should be returned
**Validates: Requirements 1.5**

## Error Handling

### Current Gap in Workflow
- **Missing Interface**: Post office staff have no way to see approved returns
- **No Receipt Confirmation**: No mechanism for post office staff to confirm package receipt
- **Workflow Bottleneck**: Returns get stuck after store staff approval
- **No Visibility**: Post office staff cannot track or manage their assigned returns

### Improved Workflow
- **Dedicated Interface**: Complete post office staff dashboard and return management
- **Real-time Updates**: Returns appear immediately after store staff approval
- **Receipt Confirmation**: Photo upload and notes for package receipt
- **Access Control**: Role-based access ensuring post office staff only see their returns
- **Search and Filter**: Easy way to find specific returns

## Testing Strategy

### Unit Testing Approach
- Test controller methods for post office staff endpoints
- Verify service methods filter returns by post office correctly
- Test receipt confirmation logic and status updates
- Validate access control prevents unauthorized access

### Property-Based Testing Approach
- Use **Spring Boot Test** framework for integration testing
- Generate random ReturnRequest entities assigned to different post offices
- Test with various post office staff users and verify data isolation
- Property tests should run a minimum of 100 iterations

**Property-based testing requirements**:
- Library: Spring Boot Test with MockMvc
- Minimum iterations: 100 per property test
- Test tagging format: `**Feature: post-office-return-workflow, Property {number}: {property_text}**`

### Integration Testing
- Test complete workflow from store approval to post office receipt confirmation
- Verify file upload functionality for receipt photos
- Test search functionality across different scenarios
- Confirm role-based access control works end-to-end

### Manual Testing
- Test post office staff login and dashboard access
- Verify returns appear after store staff approval
- Test receipt confirmation with actual photo uploads
- Confirm search functionality works with real data