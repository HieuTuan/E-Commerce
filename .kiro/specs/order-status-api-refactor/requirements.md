# Requirements Document

## Introduction

Refactor OrderStatusApiController để tuân thủ đúng kiến trúc 3 layer (Controller → Service → Repository). Hiện tại OrderStatusApiController đang vi phạm nguyên tắc này bằng cách inject và gọi trực tiếp UserRepository thay vì thông qua Service layer.

## Glossary

- **OrderStatusApiController**: REST API Controller xử lý các request liên quan đến order status
- **Service Layer**: Lớp business logic nằm giữa Controller và Repository
- **Repository Layer**: Lớp truy cập dữ liệu trực tiếp với database
- **3-Layer Architecture**: Kiến trúc phần mềm với 3 lớp: Controller → Service → Repository
- **UserRepository**: Repository để truy cập dữ liệu User
- **UserService**: Service xử lý business logic liên quan đến User (đã tồn tại)

## Requirements

### Requirement 1

**User Story:** As a developer, I want OrderStatusApiController to follow 3-layer architecture, so that the codebase is maintainable and follows best practices.

#### Acceptance Criteria

1. WHEN OrderStatusApiController needs user data THEN the system SHALL call UserService instead of UserRepository
2. WHEN any controller needs data access THEN the system SHALL NOT inject Repository classes directly
3. THE system SHALL ensure all business logic remains in the Service layer
4. THE system SHALL maintain backward compatibility with existing functionality
5. THE refactored OrderStatusApiController SHALL only inject Service classes, not Repository classes

### Requirement 2

**User Story:** As a developer, I want UserService to provide user lookup by username, so that OrderStatusApiController doesn't need direct repository access.

#### Acceptance Criteria

1. WHEN UserService already exists THEN the system SHALL verify it has a findByUsername method
2. WHEN UserService is missing findByUsername method THEN the system SHALL add it
3. WHEN OrderStatusApiController needs to find user by username THEN UserService SHALL provide the required functionality
4. THE UserService SHALL handle all user-related business logic
5. THE UserService SHALL inject UserRepository for data access

### Requirement 3

**User Story:** As a developer, I want OrderStatusApiController refactored to use UserService only, so that it follows proper layering.

#### Acceptance Criteria

1. WHEN OrderStatusApiController is refactored THEN the system SHALL remove UserRepository injection
2. WHEN OrderStatusApiController needs user data THEN the system SHALL use UserService
3. WHEN OrderStatusApiController is refactored THEN the system SHALL maintain all existing functionality
4. THE refactored OrderStatusApiController SHALL only inject Service classes, not Repository classes
5. THE system SHALL ensure the confirmDelivery endpoint continues to work correctly
