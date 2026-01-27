# Design Document - Forgot Password with OTP

## Overview

This design document outlines the enhancement of the existing forgot password functionality to use OTP (One-Time Password) verification instead of email reset tokens. The system will integrate with the existing OTP service and provide a seamless user experience for password recovery.

## Architecture

The forgot password with OTP system follows a three-step process:

1. **Email Collection**: User enters email address
2. **OTP Verification**: User enters OTP received via email  
3. **Password Reset**: User sets new password after successful OTP verification

The system leverages existing components:
- `OTPService` for OTP generation and validation
- `EmailService` for sending OTP emails
- `UserService` for password updates
- Existing authentication controllers and templates

## Components and Interfaces

### Controller Layer
- **AuthController**: Enhanced with OTP-based forgot password endpoints
  - `GET /forgot-password-otp`: Display email input form
  - `POST /forgot-password-otp`: Process email and send OTP
  - `GET /verify-reset-otp`: Display OTP verification form
  - `POST /verify-reset-otp`: Validate OTP and proceed to password reset
  - `GET /reset-password-otp`: Display password reset form (after OTP verification)
  - `POST /reset-password-otp`: Process new password
  - `POST /api/resend-reset-otp`: Resend OTP for password reset

### Service Layer
- **OTPService**: Existing service with methods:
  - `generateAndSendOTP(String email)`: Generate and send OTP
  - `validateOTP(String email, String otp)`: Validate OTP
  - `resendOTP(String email)`: Resend OTP with cooldown
  - `getRemainingAttempts(String email)`: Get remaining validation attempts
  - `isBlocked(String email)`: Check if email is blocked

- **UserService**: Enhanced with:
  - `updatePassword(String email, String newPassword)`: Update user password

### Data Models

#### Session Data
```java
// Stored in HTTP session during password reset flow
public class PasswordResetSession {
    private String email;
    private boolean otpVerified;
    private LocalDateTime createdAt;
}
```

#### OTP Configuration
- **Expiration Time**: 5 minutes
- **Max Attempts**: 3 attempts per OTP
- **Block Duration**: 5 minutes after max attempts exceeded
- **Resend Cooldown**: 60 seconds between resend requests
- **OTP Format**: 6-digit numeric code

## Error Handling

### Email Validation Errors
- Invalid email format
- Empty email input
- Email not found (generic message for security)

### OTP Validation Errors
- Invalid OTP code
- Expired OTP
- Maximum attempts exceeded
- Account temporarily blocked

### Password Reset Errors
- Password doesn't meet security requirements
- Password confirmation mismatch
- Session expired or invalid

### System Errors
- Email service unavailable
- OTP service failure
- Database connection issues

## Testing Strategy

The testing approach combines unit tests for specific functionality and property-based tests for universal behaviors:

**Property-Based Testing Library**: JUnit 5 with jqwik for Java property-based testing

**Unit Testing**:
- Controller endpoint responses
- Service method behaviors with specific inputs
- Template rendering with various data states
- Error handling scenarios

**Property-Based Testing**:
- OTP generation and validation across random inputs
- Email format validation across various email patterns
- Password security validation across different password combinations
- Session management across different timing scenarios

**Integration Testing**:
- End-to-end password reset flow
- Email delivery verification
- Session state management
- Security measure enforcement

Each property-based test will run a minimum of 100 iterations to ensure comprehensive coverage of the input space.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

**Property 1: Valid email OTP generation**
*For any* valid email address, when submitted through the forgot password form, the system should generate and send an OTP
**Validates: Requirements 1.2**

**Property 2: Invalid email rejection**
*For any* invalid email format, when submitted through the forgot password form, the system should display an error message and prevent processing
**Validates: Requirements 1.3**

**Property 3: Non-existent email security response**
*For any* email address that doesn't exist in the system, when submitted through the forgot password form, the system should display a generic success message
**Validates: Requirements 1.4**

**Property 4: OTP verification redirect**
*For any* valid OTP entered within the time limit, the system should redirect to the password reset form
**Validates: Requirements 2.2**

**Property 5: Invalid OTP error handling**
*For any* invalid OTP code, the system should display an error message and decrement the remaining attempts counter
**Validates: Requirements 2.3**

**Property 6: Valid password update**
*For any* new password that meets security requirements, when entered after successful OTP verification, the system should update the user's password
**Validates: Requirements 3.2**

**Property 7: Password mismatch validation**
*For any* pair of passwords that don't match, the system should display an error message and prevent password update
**Validates: Requirements 3.3**

**Property 8: Invalid password validation**
*For any* password that doesn't meet security requirements, the system should display specific validation errors
**Validates: Requirements 3.4**

**Property 9: Successful reset redirect**
*For any* successful password reset, the system should redirect to the login page with a success message
**Validates: Requirements 3.5**

**Property 10: OTP resend after cooldown**
*For any* resend request made after the cooldown period has elapsed, the system should generate and send a new OTP
**Validates: Requirements 4.3**

**Property 11: State reset on new OTP**
*For any* successful OTP resend, the system should reset both the attempt counter and expiration timer
**Validates: Requirements 4.4**

**Property 12: OTP expiration timing**
*For any* generated OTP, it should expire exactly 5 minutes after creation
**Validates: Requirements 5.1**

**Property 13: OTP invalidation after use**
*For any* OTP that has been successfully used for password reset, it should be immediately invalidated and cannot be reused
**Validates: Requirements 5.4**