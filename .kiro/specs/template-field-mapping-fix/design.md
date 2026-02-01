# Design Document

## Overview

This design addresses the template field mapping error in the staff returns detail page by correcting the field reference from `phoneNumber` to `phone` in the Thymeleaf template. The solution ensures proper field mapping between the User entity and template expressions while maintaining null safety and user-friendly display.

## Architecture

The fix involves a simple template correction within the existing Spring Boot MVC architecture:

```
Controller -> Service -> Repository -> Entity
                |
                v
         Thymeleaf Template (Fix field mapping here)
                |
                v
           Rendered HTML
```

The architecture remains unchanged - only the template field reference needs correction.

## Components and Interfaces

### Affected Components

1. **Template File**: `src/main/resources/templates/staff/returns/detail.html`
   - Current problematic expression: `${returnRequest.order.user.phoneNumber ?: 'Chưa có'}`
   - Corrected expression: `${returnRequest.order.user.phone ?: 'Chưa có'}`

2. **User Entity**: `src/main/java/com/mypkga/commerceplatformfull/entity/User.java`
   - Field name: `phone` (String type, nullable)
   - No changes needed to entity

### Interface Contracts

- **Template Expression Contract**: Template expressions must reference actual entity field names
- **Null Safety Contract**: Use Elvis operator (`?:`) for nullable fields with appropriate fallback values
- **Display Contract**: Show user-friendly messages when field values are null or empty

## Data Models

### User Entity Field Mapping

```java
public class User {
    // ... other fields
    
    @Column(length = 20)
    private String phone;  // <- This is the correct field name
    
    // ... other fields
}
```

### Template Variable Mapping

```html
<!-- Current (incorrect) -->
<span th:text="${returnRequest.order.user.phoneNumber ?: 'Chưa có'}">

<!-- Corrected -->
<span th:text="${returnRequest.order.user.phone ?: 'Chưa có'}">
```
## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following properties can be tested:

**Property 1: Phone number display consistency**
*For any* User entity with a non-null phone field, when the return request detail template is rendered, the displayed phone number should match the entity's phone field value
**Validates: Requirements 1.2**

**Property 2: Template rendering without exceptions**
*For any* valid return request with associated user data, when the template is processed, no SpelEvaluationException should be thrown during field access
**Validates: Requirements 1.4**

**Property 3: Elvis operator fallback behavior**
*For any* User entity with null or empty phone field, when the template expression with Elvis operator is evaluated, the fallback value should be displayed
**Validates: Requirements 2.4**

## Error Handling

### Current Error Scenario
- **Error Type**: `SpelEvaluationException`
- **Root Cause**: Field name mismatch (`phoneNumber` vs `phone`)
- **Impact**: Complete page rendering failure
- **User Experience**: 500 Internal Server Error

### Improved Error Handling
- **Prevention**: Correct field name mapping
- **Validation**: Template expressions reference valid entity fields
- **Fallback**: Elvis operator provides default values for null fields
- **Logging**: Maintain existing Spring Boot error logging

## Testing Strategy

### Unit Testing Approach
- Test template rendering with mock return request data
- Verify correct phone number display with various phone field values
- Test null/empty phone field scenarios
- Validate no exceptions are thrown during template processing

### Property-Based Testing Approach
- Use **Thymeleaf testing framework** for template testing
- Generate random User entities with various phone field values (null, empty, valid numbers)
- Property tests should run a minimum of 100 iterations
- Each property-based test will be tagged with comments referencing the design document properties

**Property-based testing requirements**:
- Library: Spring Boot Test with Thymeleaf testing support
- Minimum iterations: 100 per property test
- Test tagging format: `**Feature: template-field-mapping-fix, Property {number}: {property_text}**`

### Integration Testing
- Test complete return request detail page rendering
- Verify end-to-end template processing with real database entities
- Test with various user data scenarios (with/without phone numbers)

### Manual Testing
- Access return request detail pages through the web interface
- Verify phone numbers display correctly
- Confirm no 500 errors occur
- Test with different user profiles (with and without phone data)