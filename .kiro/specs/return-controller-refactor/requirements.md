# Requirements Document

## Introduction

Refactor ReturnWebController để tuân thủ đúng kiến trúc 3 layer (Controller → Service → Repository). Hiện tại ReturnWebController đang vi phạm nguyên tắc này bằng cách inject và gọi trực tiếp PostOfficeRepository và OrderRepository thay vì thông qua Service layer.

## Glossary

- **ReturnWebController**: Web Controller xử lý các request liên quan đến return requests (render HTML views)
- **Service Layer**: Lớp business logic nằm giữa Controller và Repository
- **Repository Layer**: Lớp truy cập dữ liệu trực tiếp với database
- **3-Layer Architecture**: Kiến trúc phần mềm với 3 lớp: Controller → Service → Repository
- **PostOfficeRepository**: Repository để truy cập dữ liệu PostOffice
- **OrderRepository**: Repository để truy cập dữ liệu Order
- **PostOfficeService**: Service xử lý business logic liên quan đến PostOffice (đã tồn tại)
- **OrderService**: Service xử lý business logic liên quan đến Order (đã tồn tại)

## Requirements

### Requirement 1

**User Story:** As a developer, I want ReturnWebController to follow 3-layer architecture, so that the codebase is maintainable and follows best practices.

#### Acceptance Criteria

1. WHEN ReturnWebController needs post office data THEN the system SHALL call PostOfficeService instead of PostOfficeRepository
2. WHEN ReturnWebController needs order data THEN the system SHALL call OrderService instead of OrderRepository
3. WHEN any controller needs data access THEN the system SHALL NOT inject Repository classes directly
4. THE system SHALL ensure all business logic remains in the Service layer
5. THE system SHALL maintain backward compatibility with existing functionality

### Requirement 2

**User Story:** As a developer, I want OrderService to provide methods for order retrieval and validation, so that order-related operations are centralized in the service layer.

#### Acceptance Criteria

1. WHEN OrderService provides order retrieval THEN the system SHALL include a method to get order by ID with user ownership verification
2. WHEN an order does not exist THEN OrderService SHALL throw an appropriate exception
3. WHEN a user attempts to access an order they do not own THEN OrderService SHALL throw an access denied exception
4. WHEN checking if an order has a return request THEN OrderService SHALL provide this functionality
5. THE OrderService SHALL inject OrderRepository for data access

### Requirement 3

**User Story:** As a developer, I want ReturnWebController refactored to use only Service classes, so that it follows proper layering.

#### Acceptance Criteria

1. WHEN ReturnWebController is refactored THEN the system SHALL remove PostOfficeRepository injection
2. WHEN ReturnWebController is refactored THEN the system SHALL remove OrderRepository injection
3. WHEN ReturnWebController needs post office data THEN the system SHALL use PostOfficeService
4. WHEN ReturnWebController needs order data THEN the system SHALL use OrderService
5. THE refactored ReturnWebController SHALL only inject Service classes, not Repository classes
