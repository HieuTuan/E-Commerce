# Design Document

## Overview

This design refactors OrderStatusApiController to follow proper 3-layer architecture by removing direct UserRepository dependency and routing all data access through UserService. The refactoring is straightforward since UserService already has the required `findByUsername` method.

## Architecture

The system follows a standard 3-layer architecture:

```
Controller Layer (OrderStatusApiController)
    ↓
Service Layer (UserService, OrderStatusManager, UIIntegrationService)
    ↓
Repository Layer (UserRepository)
```

**Current Issues:**
- OrderStatusApiController directly injects UserRepository
- Controller bypasses service layer for user lookup

**Solution:**
- Remove UserRepository injection from OrderStatusApiController
- Use existing UserService.findByUsername() method for user lookup
- Maintain all existing functionality

## Components and Interfaces

### 1. OrderStatusApiController (Refactored)

**Remove:**
- `private final UserRepository userRepository;`

**Add:**
- `private final UserService userService;`

**Update method calls in confirmDelivery:**
- `userRepository.findByUsername(username)` → `userService.findByUsername(username)`

### 2. UserService (No Changes Needed)

UserService already has the required method:
```java
Optional<User> findByUsername(String username);
```

## Data Models

No changes to existing entity models (User, Order).

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Service layer encapsulation
*For any* controller class, it should not directly inject or call repository classes
**Validates: Requirements 1.2, 3.4**

### Property 2: User lookup delegation
*For any* user lookup operation in OrderStatusApiController, the operation should be delegated to UserService
**Validates: Requirements 1.1, 2.3, 3.2**

### Property 3: Functional equivalence
*For any* operation that existed before refactoring, the refactored version should produce the same result
**Validates: Requirements 1.4, 3.3**

## Error Handling

- Maintain existing exception handling patterns
- No changes to existing error handling behavior
- UserService.findByUsername() returns Optional<User>, same as UserRepository

## Testing Strategy

### Unit Tests

We will write unit tests for:
- Verify UserService is called correctly from OrderStatusApiController
- Verify confirmDelivery endpoint works with UserService

### Property-Based Tests

We will use **jqwik** (Java property-based testing library) for property-based testing.

Each property-based test will:
- Run a minimum of 100 iterations
- Be tagged with a comment referencing the correctness property using format: `**Feature: order-status-api-refactor, Property {number}: {property_text}**`
- Each correctness property will be implemented by a SINGLE property-based test

**Property Tests to Implement:**

1. **Property 1: Service layer encapsulation**
   - Use reflection to verify OrderStatusApiController does not have any Repository field injections
   - Verify only Service classes are injected

2. **Property 3: Functional equivalence**
   - Test that user lookup operations produce same results through service as they would through repository

### Testing Framework Configuration

- Framework: jqwik (already present in project)
- Minimum iterations per property test: 100
- Test location: `src/test/java/com/mypkga/commerceplatformfull/`
