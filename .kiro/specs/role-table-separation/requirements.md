# Requirements Document

## Introduction

Cải thiện hệ thống quản lý người dùng bằng cách tách bảng Role riêng biệt để quản lý quyền hạn người dùng một cách linh hoạt và có thể mở rộng. Đồng thời loại bỏ hoàn toàn trường email khỏi hệ thống để đơn giản hóa quy trình đăng ký.

## Glossary

- **User_Management_System**: Hệ thống quản lý tài khoản và quyền hạn người dùng
- **Username**: Tên đăng nhập duy nhất để xác thực người dùng
- **Password**: Mật khẩu để xác thực người dùng
- **Role_Entity**: Bảng chứa thông tin về các vai trò trong hệ thống
- **User_Role_Relationship**: Mối quan hệ 1-n giữa Role và User

## Requirements

### Requirement 1

**User Story:** As a system administrator, I want to manage user roles through a separate Role table, so that I can easily add, modify, or remove roles without affecting the user table structure.

#### Acceptance Criteria

1. WHEN creating a new role THEN the User_Management_System SHALL store role information in the Role_Entity table
2. WHEN assigning a role to a user THEN the User_Management_System SHALL create a User_Role_Relationship linking the user to the role
3. WHEN a role is updated THEN the User_Management_System SHALL apply changes to all users with that role automatically
4. WHEN a role is deleted THEN the User_Management_System SHALL handle the deletion gracefully and reassign users to a default role
5. THE User_Management_System SHALL support multiple users having the same role through the 1-n relationship

### Requirement 2

**User Story:** As a system architect, I want a normalized database structure for roles, so that role management is scalable and maintainable.

#### Acceptance Criteria

1. WHEN the Role_Entity is created THEN the User_Management_System SHALL include fields for role name, description, and permissions
2. WHEN users are created THEN the User_Management_System SHALL reference the Role_Entity through a foreign key relationship
3. WHEN querying user permissions THEN the User_Management_System SHALL join User and Role tables efficiently
4. WHEN migrating existing data THEN the User_Management_System SHALL convert current role enum values to Role_Entity records
5. THE User_Management_System SHALL maintain referential integrity between User and Role_Entity tables

### Requirement 3

**User Story:** As a developer, I want backward compatibility during the role table migration, so that existing functionality continues to work during and after the transition.

#### Acceptance Criteria

1. WHEN the migration runs THEN the User_Management_System SHALL preserve all existing user role assignments
2. WHEN the application starts THEN the User_Management_System SHALL work with both old and new role structures during transition
3. WHEN security checks are performed THEN the User_Management_System SHALL recognize roles from the new Role_Entity table
4. WHEN role-based access control is applied THEN the User_Management_System SHALL use the new role structure consistently
5. THE User_Management_System SHALL provide a rollback mechanism in case migration issues occur

### Requirement 4

**User Story:** As a potential customer, I want to register an account with only username and password, so that I can quickly create an account without providing additional personal information.

#### Acceptance Criteria

1. WHEN a user accesses the registration form THEN the User_Management_System SHALL display only Username and Password fields as required
2. WHEN a user submits the registration form with valid username and password THEN the User_Management_System SHALL create the account successfully
3. WHEN a user submits the registration form with invalid data THEN the User_Management_System SHALL reject the registration and display validation errors
4. THE User_Management_System SHALL assign a default role to newly registered users
5. THE User_Management_System SHALL not require any email or additional contact information for registration

### Requirement 5

**User Story:** As a system administrator, I want to completely remove email functionality from the system, so that the application is simplified and focused on core e-commerce features.

#### Acceptance Criteria

1. WHEN the system is updated THEN the User_Management_System SHALL remove all email fields from the User entity
2. WHEN users interact with the system THEN the User_Management_System SHALL not request or store any email information
3. WHEN notifications are needed THEN the User_Management_System SHALL use alternative methods such as in-app notifications
4. WHEN orders are processed THEN the User_Management_System SHALL handle confirmations without email notifications
5. THE User_Management_System SHALL remove all email-related validation and processing logic