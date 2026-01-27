# Implementation Plan

- [x] 1. Update AuthController with OTP-based forgot password endpoints


  - Add new endpoints for OTP-based password reset flow
  - Implement session management for password reset process
  - Add proper error handling and validation
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_




- [x] 2. Create OTP verification page and controller methods


  - Implement GET and POST endpoints for OTP verification
  - Add session validation and OTP validation logic
  - Handle OTP expiration and attempt limiting
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_



- [x] 3. Implement password reset form and processing


  - Create password reset endpoint after OTP verification
  - Add password validation and confirmation logic
  - Integrate with UserService for password updates
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_



- [x] 4. Add OTP resend functionality


  - Implement resend OTP API endpoint
  - Add cooldown and rate limiting logic
  - Handle resend button state management
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_


- [x] 5. Create HTML templates for OTP-based password reset


  - Create forgot-password-otp.html template
  - Create verify-reset-otp.html template
  - Create reset-password-otp.html template
  - Add proper styling and JavaScript for user experience
  - _Requirements: 1.1, 2.1, 3.1, 4.1_

- [x] 6. Update login page to use new OTP-based forgot password


  - Modify login.html to link to new OTP-based forgot password flow
  - Ensure backward compatibility if needed
  - _Requirements: 1.1_

- [x] 7. Add security measures and validation


  - Implement OTP expiration timing
  - Add OTP invalidation after successful use
  - Ensure proper session security
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_



- [x] 9. Checkpoint - Ensure all tests pass



  - Ensure all tests pass, ask the user if questions arise.

