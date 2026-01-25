# Requirements Document

## Introduction

Cải thiện hệ thống người dùng bằng cách: (1) làm cho trường email trở thành tùy chọn trong đăng ký để giảm rào cản và tăng tỷ lệ chuyển đổi, và (2) tách bảng Role riêng biệt để quản lý quyền hạn người dùng một cách linh hoạt và có thể mở rộng.

## Glossary

- **User_Management_System**: Hệ thống quản lý tài khoản và quyền hạn người dùng
- **Email_Field**: Trường nhập địa chỉ email trong form đăng ký
- **Username**: Tên đăng nhập duy nhất để xác thực người dùng
- **Password**: Mật khẩu để xác thực người dùng
- **Optional_Field**: Trường thông tin không bắt buộc phải nhập
- **Role_Entity**: Bảng chứa thông tin về các vai trò trong hệ thống
- **User_Role_Relationship**: Mối quan hệ 1-n giữa Role và User

## Requirements

### Requirement 1

**User Story:** As a potential customer, I want to register an account without providing an email address, so that I can quickly create an account without sharing personal information.

#### Acceptance Criteria

1. WHEN a user accesses the registration form THEN the User_Management_System SHALL display the Email_Field as optional
2. WHEN a user submits the registration form without an email THEN the User_Management_System SHALL create the account successfully
3. WHEN a user submits the registration form with a valid email THEN the User_Management_System SHALL store the email and create the account
4. WHEN a user submits the registration form with an invalid email format THEN the User_Management_System SHALL reject the registration and display validation error
5. THE User_Management_System SHALL require only Username and Password as mandatory fields

### Requirement 2

**User Story:** As a system administrator, I want to maintain data integrity when email is optional, so that the system continues to function properly with or without user email addresses.

#### Acceptance Criteria

1. WHEN the database stores user records THEN the User_Management_System SHALL allow null values for email fields
2. WHEN the system sends notifications THEN the User_Management_System SHALL handle users without email addresses gracefully
3. WHEN displaying user profiles THEN the User_Management_System SHALL show appropriate placeholder text for missing email addresses
4. WHEN users attempt to use email-dependent features THEN the User_Management_System SHALL prompt them to add an email address
5. THE User_Management_System SHALL maintain backward compatibility with existing user accounts that have email addresses

### Requirement 3

**User Story:** As a user without an email address, I want to be able to use core shopping features, so that I can purchase products without providing an email.

#### Acceptance Criteria

1. WHEN a user without email adds items to cart THEN the User_Management_System SHALL allow normal cart operations
2. WHEN a user without email proceeds to checkout THEN the User_Management_System SHALL allow order placement
3. WHEN a user without email completes a purchase THEN the User_Management_System SHALL process the order successfully
4. WHEN a user without email views order history THEN the User_Management_System SHALL display their orders normally
5. THE User_Management_System SHALL provide alternative contact methods for order confirmations when email is not available

### Requirement 4

**User Story:** As a developer, I want clear validation rules for optional email fields, so that the system handles email input consistently across all forms.

#### Acceptance Criteria

1. WHEN email validation is performed THEN the User_Management_System SHALL accept empty/null email values as valid
2. WHEN email validation is performed on non-empty values THEN the User_Management_System SHALL enforce proper email format rules
3. WHEN updating user profiles THEN the User_Management_System SHALL apply the same optional email validation rules
4. WHEN importing user data THEN the User_Management_System SHALL handle records with missing email fields
5. THE User_Management_System SHALL provide consistent error messages for email validation across all interfaces

### Requirement 5

**User Story:** As a system administrator, I want to manage user roles through a separate Role table, so that I can easily add, modify, or remove roles without affecting the user table structure.

#### Acceptance Criteria

1. WHEN creating a new role THEN the User_Management_System SHALL store role information in the Role_Entity table
2. WHEN assigning a role to a user THEN the User_Management_System SHALL create a User_Role_Relationship linking the user to the role
3. WHEN a role is updated THEN the User_Management_System SHALL apply changes to all users with that role automatically
4. WHEN a role is deleted THEN the User_Management_System SHALL handle the deletion gracefully and reassign users to a default role
5. THE User_Management_System SHALL support multiple users having the same role through the 1-n relationship

### Requirement 6

**User Story:** As a system architect, I want a normalized database structure for roles, so that role management is scalable and maintainable.

#### Acceptance Criteria

1. WHEN the Role_Entity is created THEN the User_Management_System SHALL include fields for role name, description, and permissions
2. WHEN users are created THEN the User_Management_System SHALL reference the Role_Entity through a foreign key relationship
3. WHEN querying user permissions THEN the User_Management_System SHALL join User and Role tables efficiently
4. WHEN migrating existing data THEN the User_Management_System SHALL convert current role enum values to Role_Entity records
5. THE User_Management_System SHALL maintain referential integrity between User and Role_Entity tables

### Requirement 7

**User Story:** As a developer, I want backward compatibility during the role table migration, so that existing functionality continues to work during and after the transition.

#### Acceptance Criteria

1. WHEN the migration runs THEN the User_Management_System SHALL preserve all existing user role assignments
2. WHEN the application starts THEN the User_Management_System SHALL work with both old and new role structures during transition
3. WHEN security checks are performed THEN the User_Management_System SHALL recognize roles from the new Role_Entity table
4. WHEN role-based access control is applied THEN the User_Management_System SHALL use the new role structure consistently
5. THE User_Management_System SHALL provide a rollback mechanism in case migration issues occur