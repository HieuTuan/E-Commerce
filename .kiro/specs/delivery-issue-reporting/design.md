# Design Document - Delivery Issue Reporting System

## Overview

Hệ thống báo cáo vấn đề giao hàng cho phép khách hàng báo cáo khi chưa nhận được hàng và admin quản lý các báo cáo này. Hệ thống sẽ cập nhật trạng thái đơn hàng với visual indicator và đồng bộ trạng thái real-time.

## Architecture

### High-Level Architecture

```
Customer UI → DeliveryIssueController → DeliveryIssueService → Database
                                    ↓
Admin UI ← DeliveryIssueController ← OrderService ← Database
```

### Component Interaction

1. **Customer Flow**: Customer reports issue → Creates DeliveryIssueReport → Updates Order flag
2. **Admin Flow**: Admin views reports → Updates report status → Updates Order status
3. **Status Sync**: Real-time status updates across all interfaces

## Components and Interfaces

### 1. DeliveryIssueReport Entity
```java
@Entity
public class DeliveryIssueReport {
    private Long id;
    private Long orderId;
    private String customerEmail;
    private String issueType; // "CHƯA_NHẬN_ĐƯỢC_HÀNG"
    private String description;
    private ReportStatus status; // PENDING, RESOLVED, REJECTED
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String adminNotes;
}
```

### 2. Order Entity Updates
```java
@Entity
public class Order {
    // Existing fields...
    
    @Column(name = "has_delivery_issue")
    private Boolean hasDeliveryIssue = false;
    
    @OneToMany(mappedBy = "orderId")
    private List<DeliveryIssueReport> deliveryIssues;
}
```

### 3. DeliveryIssueService Interface
```java
public interface DeliveryIssueService {
    DeliveryIssueReport createReport(Long orderId, String customerEmail, String issueType, String description);
    List<DeliveryIssueReport> getPendingReports();
    DeliveryIssueReport resolveReport(Long reportId, String adminEmail, String notes);
    DeliveryIssueReport rejectReport(Long reportId, String adminEmail, String notes);
    List<DeliveryIssueReport> getReportsByOrderId(Long orderId);
}
```

### 4. Controller Endpoints
```java
@RestController
@RequestMapping("/api/delivery-issues")
public class DeliveryIssueController {
    @PostMapping("/report")
    ResponseEntity<?> reportIssue(@RequestBody ReportRequest request);
    
    @GetMapping("/pending")
    ResponseEntity<List<DeliveryIssueReport>> getPendingReports();
    
    @PutMapping("/{id}/resolve")
    ResponseEntity<?> resolveReport(@PathVariable Long id, @RequestBody ResolveRequest request);
    
    @PutMapping("/{id}/reject")
    ResponseEntity<?> rejectReport(@PathVariable Long id, @RequestBody RejectRequest request);
}
```

## Data Models

### DeliveryIssueReport
- **id**: Primary key
- **orderId**: Foreign key to Order
- **customerEmail**: Email của khách hàng báo cáo
- **issueType**: Loại vấn đề (chỉ "CHƯA_NHẬN_ĐƯỢC_HÀNG")
- **description**: Mô tả chi tiết (optional)
- **status**: PENDING, RESOLVED, REJECTED
- **reportedAt**: Thời gian báo cáo
- **resolvedAt**: Thời gian giải quyết
- **resolvedBy**: Admin xử lý
- **adminNotes**: Ghi chú của admin

### Order Updates
- **hasDeliveryIssue**: Boolean flag để đánh dấu đơn hàng có vấn đề
- **deliveryIssues**: One-to-many relationship với DeliveryIssueReport

### ReportStatus Enum
```java
public enum ReportStatus {
    PENDING("Đang xử lý"),
    RESOLVED("Đã giải quyết"), 
    REJECTED("Đã từ chối");
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After reviewing all acceptance criteria, I identified several redundant properties that can be consolidated:
- Properties 2.3 and 6.4 both test report resolution - can be combined
- Properties 1.4 and 3.1 both test delivery issue display - can be combined  
- Properties 7.2, 7.3, 7.4 can be combined into one comprehensive status display property
- Properties 3.3 and 3.5 are not testable (real-time sync across multiple UIs)

### Core Properties

**Property 1: Report creation updates order state**
*For any* order with DELIVERED status, when a delivery issue report is created, the order's has_delivery_issue flag should be set to true and a PENDING report should be created
**Validates: Requirements 1.2, 1.3**

**Property 2: Delivery issue visual indicator**
*For any* order with has_delivery_issue = true, the system should display "DELIVERED" status in red color with "(Khách hàng report)" text in both customer and admin interfaces
**Validates: Requirements 1.4, 3.1**

**Property 3: Report button state management**
*For any* order that has an existing delivery issue report, the "Báo cáo" button should be disabled and display "Đã báo cáo vấn đề" text
**Validates: Requirements 1.5**

**Property 4: Admin report resolution**
*For any* delivery issue report, when admin marks it as RESOLVED, both the report status should become RESOLVED and the order's has_delivery_issue flag should be set to false
**Validates: Requirements 2.3, 6.4**

**Property 5: Admin report rejection**
*For any* delivery issue report, when admin marks it as REJECTED, the report status should become REJECTED but the order's has_delivery_issue flag should remain true
**Validates: Requirements 2.4**

**Property 6: Report data persistence**
*For any* delivery issue report creation, all required fields (order_id, customer_email, issue_type, description, reported_at, status) should be saved to the database
**Validates: Requirements 5.1**

**Property 7: Admin processing updates**
*For any* delivery issue report that gets processed by admin, the resolved_at, resolved_by, and admin_notes fields should be updated accordingly
**Validates: Requirements 5.2**

**Property 8: Timeline entry creation**
*For any* delivery issue report status change, a corresponding timeline entry should be created for the associated order
**Validates: Requirements 5.3**

**Property 9: Customer status display**
*For any* delivery issue report, the customer should see the appropriate status message: "Đang xử lý" for PENDING, "Đã giải quyết" for RESOLVED, or "Đã từ chối" for REJECTED
**Validates: Requirements 7.1, 7.2, 7.3, 7.4**

**Property 10: Notification sending**
*For any* delivery issue report status change, an email notification should be sent to the customer
**Validates: Requirements 2.5, 7.5**

**Property 11: Admin detail view completeness**
*For any* delivery issue report detail view, all required information (order details, customer info, issue reason) should be displayed
**Validates: Requirements 2.2, 6.2**

**Property 12: Admin action link visibility**
*For any* order with has_delivery_issue = true, the admin interface should display a "Xem báo cáo vấn đề" link in the actions column
**Validates: Requirements 3.4**

## Error Handling

### Input Validation
- **Report Creation**: Validate that order exists and has DELIVERED status
- **Customer Authorization**: Verify customer owns the order before allowing report creation
- **Admin Authorization**: Verify admin permissions before allowing report processing
- **Duplicate Reports**: Prevent multiple reports for the same order
- **Required Fields**: Validate all required fields are provided

### Error Scenarios
- **Order Not Found**: Return 404 with clear error message
- **Unauthorized Access**: Return 403 with appropriate message
- **Duplicate Report**: Return 409 with "Báo cáo đã tồn tại" message
- **Invalid Status Transition**: Prevent invalid status changes
- **Database Errors**: Handle connection issues gracefully
- **Email Sending Failures**: Log errors but don't block the main flow

### Error Recovery
- **Transaction Rollback**: Use database transactions for multi-step operations
- **Retry Mechanisms**: Implement retry for email notifications
- **Graceful Degradation**: System continues working even if notifications fail
- **Error Logging**: Comprehensive logging for debugging

## Testing Strategy

### Dual Testing Approach
The system will use both unit testing and property-based testing to ensure comprehensive coverage:

- **Unit tests** verify specific examples, edge cases, and error conditions
- **Property tests** verify universal properties that should hold across all inputs
- Together they provide comprehensive coverage: unit tests catch concrete bugs, property tests verify general correctness

### Property-Based Testing
- **Framework**: JQwik for Java property-based testing
- **Test Configuration**: Minimum 100 iterations per property test
- **Property Test Tagging**: Each property-based test must include a comment with format: `**Feature: delivery-issue-reporting, Property {number}: {property_text}**`
- **Single Property Implementation**: Each correctness property must be implemented by exactly one property-based test

### Unit Testing
- **Framework**: JUnit 5 with Spring Boot Test
- **Coverage Areas**:
  - Controller endpoint behavior
  - Service layer business logic
  - Repository operations
  - Error handling scenarios
  - Email notification functionality
- **Test Data**: Use test containers for database integration tests
- **Mocking Strategy**: Mock external dependencies (email service) but test real database operations

### Integration Testing
- **End-to-End Flows**: Test complete customer and admin workflows
- **API Testing**: Verify all REST endpoints work correctly
- **Database Integration**: Test with real database using test containers
- **UI Testing**: Selenium tests for critical user journeys

### Test Organization
- **Unit Tests**: Co-located with source files using `.test.java` suffix
- **Integration Tests**: Separate test package structure
- **Property Tests**: Dedicated package for property-based tests
- **Test Utilities**: Shared test data builders and utilities
