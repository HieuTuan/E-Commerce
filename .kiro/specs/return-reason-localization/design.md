# Design Document

## Overview

This design addresses both the return reason localization issue and return status mapping issue by updating the Thymeleaf template to:
1. Use the `displayName` property of the `ReturnReason` enum instead of displaying the raw enum name
2. Use correct enum names for `ReturnStatus` mapping and utilize the `displayName` property
The solution leverages the existing Vietnamese display names already defined in both enums without requiring any backend changes.

## Architecture

The fix involves updating template expressions within the existing Spring Boot MVC architecture:

```
Controller -> Service -> Repository -> Entity (ReturnRequest)
                |                           |
                |                           v
                |                    ReturnReason enum (with displayName)
                |                    ReturnStatus enum (with displayName)
                |                           |
                v                           v
         Thymeleaf Template (Use correct enum names and displayName)
                |
                v
           Localized HTML
```

No architectural changes are required - only template expression updates.

## Components and Interfaces

### Affected Components

1. **Template File**: `src/main/resources/templates/staff/returns/detail.html`
   - **Return Reason Fix**:
     - Current: `${returnRequest.reason}`
     - Updated: `${returnRequest.reason.displayName}`
   
   - **Return Status Fix**:
     - Current: Uses old enum names like `PENDING`, `APPROVED`, `COMPLETED`
     - Updated: Use correct enum names like `REFUND_REQUESTED`, `RETURN_APPROVED`, `REFUNDED`
     - Also use: `${returnRequest.status.displayName}` for display

2. **ReturnReason Enum**: Already contains Vietnamese display names (no changes needed)
3. **ReturnStatus Enum**: Already contains Vietnamese display names (no changes needed)

### Interface Contracts

- **Enum Display Contract**: Templates must use `displayName` property for user-facing text
- **Status Mapping Contract**: Template conditionals must use correct enum names
- **Null Safety Contract**: Handle cases where reason or status might be null
- **Localization Contract**: All enum values must display in Vietnamese

## Data Models

### ReturnReason Enum Structure

```java
public enum ReturnReason {
    DEFECTIVE_ITEM("Sản phẩm bị lỗi"),
    NOT_AS_DESCRIBED("Không đúng như mô tả"),
    WRONG_DELIVERY("Giao sai hàng"),
    DAMAGED_PACKAGING("Bao bì bị hỏng"),
    OTHER("Lý do khác");

    public String getDisplayName() {
        return displayName;  // <- Templates should use this
    }
}
```

### ReturnStatus Enum Structure

```java
public enum ReturnStatus {
    REFUND_REQUESTED("Yêu cầu hoàn trả"),
    RETURN_APPROVED("Chấp nhận hoàn trả"),
    RETURNING("Đang hoàn trả"),
    RETURN_RECEIVED("Đã nhận hàng hoàn trả"),
    REFUNDED("Đã hoàn tiền"),
    REFUND_REJECTED("Từ chối hoàn trả");

    public String getDisplayName() {
        return displayName;  // <- Templates should use this
    }
}
```

### Template Expression Mapping

#### Return Reason Fix
```html
<!-- Current (shows enum name) -->
<span th:text="${returnRequest.reason}">NOT_AS_DESCRIBED</span>

<!-- Updated (shows Vietnamese display name) -->
<span th:text="${returnRequest.reason.displayName}">Không đúng như mô tả</span>

<!-- With null safety -->
<span th:text="${returnRequest.reason?.displayName ?: 'Không xác định'}">Không đúng như mô tả</span>
```

#### Return Status Fix
```html
<!-- Current (incorrect enum names) -->
<span th:switch="${returnRequest.status.name()}">
    <span th:case="'PENDING'">Đang chờ nhân viên xem xét và duyệt</span>
    <span th:case="'APPROVED'">Đã duyệt, chờ khách hàng gửi hàng</span>
    <span th:case="'COMPLETED'">Đã hoàn tiền thành công</span>
    <span th:case="*">Trạng thái không xác định</span>
</span>

<!-- Updated (correct enum names) -->
<span th:switch="${returnRequest.status.name()}">
    <span th:case="'REFUND_REQUESTED'">Đang chờ nhân viên xem xét và duyệt</span>
    <span th:case="'RETURN_APPROVED'">Đã duyệt, chờ khách hàng gửi hàng</span>
    <span th:case="'RETURNING'">Khách hàng đang gửi hàng trả về</span>
    <span th:case="'RETURN_RECEIVED'">Đã nhận hàng, chờ hoàn tiền</span>
    <span th:case="'REFUNDED'">Đã hoàn tiền thành công</span>
    <span th:case="'REFUND_REJECTED'">Đã từ chối yêu cầu</span>
    <span th:case="*">Trạng thái không xác định</span>
</span>

<!-- Alternative: Use displayName directly -->
<span th:text="${returnRequest.status.displayName}">Yêu cầu hoàn trả</span>
```
## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, after eliminating redundancy, the following properties can be tested:

**Property 1: Return reason display name consistency**
*For any* ReturnReason enum value, when the template expression accesses the displayName property, the rendered output should match the enum's predefined Vietnamese display name
**Validates: Requirements 1.1, 3.1, 3.5**

**Property 2: Return status mapping and display consistency**
*For any* ReturnStatus enum value, when the template conditional expressions use the correct enum names, the status should be properly recognized and display the appropriate Vietnamese text
**Validates: Requirements 2.1, 3.2, 3.5**

**Property 3: Null safety fallback behavior**
*For any* return request with null reason or status values, when the template expressions with null safety operators are evaluated, the fallback text should be displayed instead of causing rendering errors
**Validates: Requirements 3.6, 4.1**

## Error Handling

### Current Error Scenarios
1. **Return Reason Issue**: English enum names displayed instead of Vietnamese text
   - Root Cause: Template using `${returnRequest.reason}` instead of `${returnRequest.reason.displayName}`
   - Impact: Poor user experience with technical English text

2. **Return Status Issue**: "Trạng thái không xác định" displayed for valid statuses
   - Root Cause: Template using incorrect enum names in switch statements (e.g., `PENDING` instead of `REFUND_REQUESTED`)
   - Impact: Staff cannot understand actual return request status

### Improved Error Handling
- **Prevention**: Use displayName property and correct enum names in template expressions
- **Validation**: Template expressions access the correct enum properties and names
- **Fallback**: Null safety operators provide default text for missing reasons/statuses
- **Consistency**: All enum displays use Vietnamese localization

## Testing Strategy

### Unit Testing Approach
- Test template rendering with mock return request data containing different ReturnReason and ReturnStatus values
- Verify Vietnamese display names appear in rendered output for both reasons and statuses
- Test null reason and status scenarios with fallback text
- Validate no English enum names appear in final HTML
- Test all ReturnStatus enum values to ensure proper switch case matching

### Property-Based Testing Approach
- Use **Thymeleaf testing framework** for template testing
- Generate random ReturnRequest entities with various ReturnReason and ReturnStatus enum values
- Property tests should run a minimum of 100 iterations
- Each property-based test will be tagged with comments referencing the design document properties

**Property-based testing requirements**:
- Library: Spring Boot Test with Thymeleaf testing support
- Minimum iterations: 100 per property test
- Test tagging format: `**Feature: return-reason-localization, Property {number}: {property_text}**`

### Integration Testing
- Test complete return request detail page rendering with real database entities
- Verify Vietnamese text appears correctly in browser for both reasons and statuses
- Test with all possible ReturnReason and ReturnStatus enum values
- Confirm no "Trạng thái không xác định" appears for valid statuses
- Confirm consistent localization across different return requests

### Manual Testing
- Access return request detail pages with different return reasons and statuses
- Verify Vietnamese text displays correctly for both fields
- Test with return requests that have null reasons or statuses
- Confirm no English enum names appear in the interface
- Verify all status transitions display proper Vietnamese text