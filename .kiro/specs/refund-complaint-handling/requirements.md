# Requirements Document

## Introduction

The Refund & Complaint Handling system enables authenticated buyers to request refunds for eligible orders and provides administrators with tools to review and process these requests. The system ensures financial integrity by validating refund amounts against original payments and maintaining comprehensive audit trails.

## Glossary

- **Buyer**: An authenticated user who owns an order and can request refunds
- **Admin**: An administrator who can review and approve/reject refund requests
- **Gateway_Payment**: Payment made through external payment providers (VNPAY, Momo, etc.)
- **Refund_System**: The complete refund and complaint handling system
- **Order_Aggregate**: The order entity with associated refund tracking fields
- **Payment_Service**: Service responsible for communicating with payment gateways
- **Refund_Entity**: Database entity storing refund request information
- **Auto_Refund_Threshold**: Configurable amount below which refunds are automatically processed
- **Refund_Period**: Configurable time window during which refunds can be requested

## Requirements

### Requirement 1: Refund Request Initiation

**User Story:** As a buyer, I want to request a refund for my order, so that I can recover payment for unsatisfactory purchases.

#### Acceptance Criteria

1. WHEN a buyer views an eligible order THEN THE Refund_System SHALL display a "Request refund" option
2. WHEN a buyer selects "Request refund" THEN THE Refund_System SHALL present a form with order details, reason field, and amount field (defaulted to full refundable amount)
3. WHEN a buyer submits a refund request THEN THE Refund_System SHALL validate the request and create a new Refund_Entity
4. WHEN a refund request is submitted THEN THE Refund_System SHALL return either immediate processing result or pending notification

### Requirement 2: Order Eligibility Validation

**User Story:** As a system administrator, I want to ensure only eligible orders can be refunded, so that the system maintains financial integrity.

#### Acceptance Criteria

1. WHEN checking order eligibility THEN THE Refund_System SHALL only allow refunds for orders with status DELIVERED, COMPLETED, or CANCELLED_WITH_PAYMENT
2. WHEN validating refund timing THEN THE Refund_System SHALL only allow requests within the configured Refund_Period from delivery or payment time
3. WHEN calculating refundable amount THEN THE Refund_System SHALL ensure total refunds do not exceed the original Gateway_Payment amount
4. WHEN processing a request THEN THE Refund_System SHALL verify the requesting user owns the order (userId equals order.ownerId)

### Requirement 3: Payment Type Restrictions

**User Story:** As a financial controller, I want to restrict refunds to gateway payments only, so that we maintain proper financial controls.

#### Acceptance Criteria

1. WHEN evaluating refund eligibility THEN THE Refund_System SHALL only allow refunds for Gateway_Payment transactions
2. WHEN a user requests refund for voucher or point payments THEN THE Refund_System SHALL reject the request with appropriate error message
3. WHEN calculating refundable amounts THEN THE Refund_System SHALL only consider Gateway_Payment amounts in calculations

### Requirement 4: Automatic Refund Processing

**User Story:** As a buyer, I want small refund requests to be processed automatically, so that I receive quick resolution for minor issues.

#### Acceptance Criteria

1. WHEN a refund request amount is below the Auto_Refund_Threshold THEN THE Refund_System SHALL process it automatically
2. WHEN processing automatic refunds THEN THE Refund_System SHALL call the Payment_Service to execute the gateway refund
3. WHEN automatic processing succeeds THEN THE Refund_System SHALL update the refund status to COMPLETED and update the Order_Aggregate refundedAmount
4. WHEN automatic processing fails THEN THE Refund_System SHALL set status to PENDING_REVIEW for manual handling

### Requirement 5: Manual Review Process

**User Story:** As an admin, I want to review large refund requests manually, so that I can prevent fraudulent or inappropriate refunds.

#### Acceptance Criteria

1. WHEN a refund request exceeds the Auto_Refund_Threshold THEN THE Refund_System SHALL set status to PENDING_REVIEW
2. WHEN a refund is pending review THEN THE Refund_System SHALL notify administrators
3. WHEN an admin reviews a request THEN THE Refund_System SHALL allow APPROVE or REJECT actions
4. WHEN an admin approves a request THEN THE Refund_System SHALL re-validate eligibility before processing
5. WHEN processing approved requests THEN THE Refund_System SHALL call Payment_Service and update status to COMPLETED upon success

### Requirement 6: Data Consistency and Concurrency

**User Story:** As a system architect, I want to prevent race conditions in refund processing, so that financial data remains consistent.

#### Acceptance Criteria

1. WHEN loading order data for refund processing THEN THE Refund_System SHALL use row-level locking (findByIdForUpdate)
2. WHEN processing refund requests THEN THE Refund_System SHALL execute all database changes within a single atomic transaction
3. WHEN calculating remaining refundable amounts THEN THE Refund_System SHALL account for all previous refunds to prevent over-refunding
4. WHEN multiple refund requests occur simultaneously THEN THE Refund_System SHALL process them sequentially to maintain data integrity

### Requirement 7: Audit Trail and History

**User Story:** As a compliance officer, I want complete audit trails for all refund activities, so that we can track and investigate refund patterns.

#### Acceptance Criteria

1. WHEN any refund status changes THEN THE Refund_System SHALL log the change with timestamp, executor, and reason
2. WHEN gateway API calls are made THEN THE Refund_System SHALL log the request and response for audit purposes
3. WHEN refunds are processed THEN THE Refund_System SHALL maintain RefundStatusHistory records
4. WHEN querying refund history THEN THE Refund_System SHALL provide complete chronological records of all changes

### Requirement 8: Payment Gateway Integration

**User Story:** As a payment processor, I want reliable integration with payment gateways, so that refunds are executed correctly.

#### Acceptance Criteria

1. WHEN processing refunds THEN THE Payment_Service SHALL communicate with the appropriate gateway (VNPAY, Momo, etc.)
2. WHEN gateway calls succeed THEN THE Refund_System SHALL update refund status and Order_Aggregate accordingly
3. WHEN gateway calls fail THEN THE Refund_System SHALL log the error and maintain PENDING status for retry
4. WHEN refunds are completed THEN THE Refund_System SHALL store the actual refunded amount from gateway response

### Requirement 9: User Authentication and Authorization

**User Story:** As a security administrator, I want proper access controls for refund operations, so that only authorized users can perform refund actions.

#### Acceptance Criteria

1. WHEN accessing refund functionality THEN THE Refund_System SHALL verify user authentication
2. WHEN buyers request refunds THEN THE Refund_System SHALL verify order ownership before allowing the request
3. WHEN admins access review functions THEN THE Refund_System SHALL verify administrative privileges
4. WHEN unauthorized access is attempted THEN THE Refund_System SHALL reject the request with appropriate error response

### Requirement 10: Configuration Management

**User Story:** As a system administrator, I want configurable refund parameters, so that I can adjust business rules without code changes.

#### Acceptance Criteria

1. THE Refund_System SHALL support configurable Auto_Refund_Threshold values
2. THE Refund_System SHALL support configurable Refund_Period durations
3. WHEN configuration changes are made THEN THE Refund_System SHALL apply them to new requests without restart
4. WHEN loading configuration THEN THE Refund_System SHALL use default values if custom configuration is unavailable