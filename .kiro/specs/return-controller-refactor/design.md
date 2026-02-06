# Design Document

## Overview

This design refactors ReturnWebController to follow proper 3-layer architecture by removing direct repository dependencies (PostOfficeRepository and OrderRepository) and routing all data access through service layer. PostOfficeService already exists, and OrderService needs to be enhanced with order retrieval and validation methods.

## Architecture

The system follows a standard 3-layer architecture:

```
Controller Layer (ReturnWebController)
    ↓
Service Layer (PostOfficeService, OrderService, ReturnService, ReturnEligibilityService, UserService)
    ↓
Repository Layer (PostOfficeRepository, OrderRepository)
```

**Current Issues:**
- ReturnWebController directly injects PostOfficeRepository
- ReturnWebController directly injects OrderRepository
- Order retrieval and ownership verification logic is in the controller instead of service layer

**Solution:**
- Use existing PostOfficeService for all post office operations
- Enhance OrderService with order retrieval and validation methods
- Remove PostOfficeRepository and OrderRepository injections from ReturnWebController
- Move order retrieval and ownership verification logic to OrderService
- Use OrderService and PostOfficeService for all data access

## Components and Interfaces

### 1. PostOfficeService (Already Exists)

PostOfficeService already exists with the following methods:
```java
public interface PostOfficeService {
    List<PostOffice> getActivePostOffices();
    List<PostOffice> searchPostOfficesByAddress(String address);
    List<PostOffice> searchPostOfficesByCity(String city);
    List<PostOffice> searchPostOffices(String searchTerm);
    Optional<PostOffice> getPostOfficeById(Long id);
}
```

### 2. OrderService (Enhance Existing)

Add new methods to OrderService interface:
```java
public interface OrderService {
    // ... existing methods ...
    
    // New methods for ReturnWebController
    Order getOrderByIdWithOwnershipCheck(Long orderId, Long userId);
    boolean orderHasReturnRequest(Long orderId);
}
```

Implementation in OrderServiceImpl:
```java
@Override
public Order getOrderByIdWithOwnershipCheck(Long orderId, Long userId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));
    
    if (!order.getUser().getId().equals(userId)) {
        throw new RuntimeException("Access denied");
    }
    
    return order;
}

@Override
public boolean orderHasReturnRequest(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));
    return order.hasReturnRequest();
}
```

### 3. ReturnWebController (Refactored)

**Remove:**
- `private final PostOfficeRepository postOfficeRepository;`
- `private final OrderRepository orderRepository;`

**Add:**
- `private final PostOfficeService postOfficeService;`
- `private final OrderService orderService;`

**Update showReturnRequestForm method:**
- Replace `orderRepository.findById()` with `orderService.getOrderByIdWithOwnershipCheck()`
- Replace `order.hasReturnRequest()` with `orderService.orderHasReturnRequest()`
- Replace `postOfficeRepository.findByActiveTrue()` with `postOfficeService.getActivePostOffices()`
- Remove order ownership verification logic (now in service)

## Data Models

No changes to existing entity models (PostOffice, Order, ReturnRequest).

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Service layer encapsulation
*For any* controller class, it should not directly inject or call repository classes
**Validates: Requirements 1.3, 3.5**

### Property 2: Order operations delegation
*For any* order operation in ReturnWebController, the operation should be delegated to OrderService
**Validates: Requirements 1.2, 2.1, 3.4**

### Property 3: Post office operations delegation
*For any* post office operation in ReturnWebController, the operation should be delegated to PostOfficeService
**Validates: Requirements 1.1, 3.3**

### Property 4: Functional equivalence
*For any* operation that existed before refactoring, the refactored version should produce the same result
**Validates: Requirements 1.5, 3.5**

### Property 5: Ownership verification
*For any* order access attempt, the system should verify user ownership through OrderService
**Validates: Requirements 2.3**

## Error Handling

- Maintain existing exception handling patterns
- Service methods should throw appropriate exceptions
- Controller should catch and handle exceptions with proper HTTP status codes
- No changes to existing error handling behavior

## Testing Strategy

### Unit Tests

We will write unit tests for:
- OrderService new methods (getOrderByIdWithOwnershipCheck, orderHasReturnRequest)
- Verify OrderService and PostOfficeService are called correctly from ReturnWebController
- Test ownership verification logic
- Test order not found scenarios
- Test access denied scenarios

### Property-Based Tests

We will use **jqwik** (Java property-based testing library) for property-based testing.

Each property-based test will:
- Run a minimum of 100 iterations
- Be tagged with a comment referencing the correctness property using format: `**Feature: return-controller-refactor, Property {number}: {property_text}**`
- Each correctness property will be implemented by a SINGLE property-based test

**Property Tests to Implement:**

1. **Property 1: Service layer encapsulation**
   - Use reflection to verify ReturnWebController does not have any Repository field injections
   - Verify only Service classes are injected

2. **Property 4: Functional equivalence**
   - Test that order retrieval operations produce same results through service as they would through repository
   - Test that post office operations produce same results through service

3. **Property 5: Ownership verification**
   - Generate random user IDs and order IDs
   - Verify that ownership check correctly allows/denies access

### Testing Framework Configuration

- Framework: jqwik (already present in project)
- Minimum iterations per property test: 100
- Test location: `src/test/java/com/mypkga/commerceplatformfull/`
