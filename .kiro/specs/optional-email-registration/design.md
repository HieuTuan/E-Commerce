# Design Document

## Overview

Thiết kế này mô tả hai cải tiến quan trọng cho hệ thống quản lý người dùng:

1. **Email tùy chọn**: Làm cho trường email trở thành tùy chọn trong đăng ký để giảm rào cản và cải thiện trải nghiệm người dùng
2. **Bảng Role riêng biệt**: Tách role thành một entity riêng với mối quan hệ 1-n với User để quản lý quyền hạn linh hoạt và có thể mở rộng

Các thay đổi này sẽ duy trì tính toàn vẹn dữ liệu và khả năng tương thích ngược trong khi cải thiện kiến trúc hệ thống.

## Architecture

Kiến trúc được cập nhật với bảng Role mới và mối quan hệ 1-n:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Registration  │───▶│   User Entity    │───▶│    Database     │
│   Controller    │    │   (Updated)      │    │   (Updated)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Registration  │    │   Validation     │    │   User Service  │
│   Form (HTML)   │    │   Service        │    │   (Updated)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │   Role Entity    │
                       │     (New)        │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │   Role Service   │
                       │     (New)        │
                       └──────────────────┘
```

### Database Relationship Diagram
```
┌─────────────────┐         ┌─────────────────┐
│      Role       │    1    │      User       │
│─────────────────│◄────────│─────────────────│
│ id (PK)         │    n    │ id (PK)         │
│ name            │         │ username        │
│ description     │         │ password        │
│ permissions     │         │ email (nullable)│
│ created_date    │         │ role_id (FK)    │
│ updated_date    │         │ created_date    │
└─────────────────┘         │ updated_date    │
                            └─────────────────┘
```

## Components and Interfaces

### 1. Role Entity (New)
- Tạo entity Role mới với các trường: id, name, description, permissions
- Thiết lập mối quan hệ 1-n với User entity
- Implement validation và business logic cho role management

### 2. User Entity Updates
- Cập nhật annotation `@Column` cho trường email để cho phép null
- Thay thế enum role bằng foreign key reference đến Role entity
- Thêm validation logic cho email tùy chọn
- Duy trì backward compatibility với dữ liệu hiện tại

### 3. Registration Controller
- Cập nhật validation logic để xử lý email tùy chọn
- Thêm error handling cho các trường hợp email không hợp lệ
- Cập nhật logic gán role mặc định cho user mới
- Duy trì existing endpoints và behavior

### 4. Registration Form (HTML)
- Loại bỏ thuộc tính `required` khỏi trường email
- Thêm placeholder text để chỉ ra email là tùy chọn
- Cập nhật client-side validation

### 5. User Service Layer
- Cập nhật business logic để xử lý users không có email
- Thêm helper methods để kiểm tra email availability
- Cập nhật notification logic
- Thêm methods để quản lý role assignments

### 6. Role Service Layer (New)
- Implement CRUD operations cho Role entity
- Provide methods để query roles và permissions
- Handle role assignment và validation logic

## Data Models

### New Role Entity
```java
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions; // JSON string or comma-separated values
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedDate;
    
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();
    
    // Constructors, getters, setters
}
```

### Updated User Entity
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @Column(name = "email", length = 100, nullable = true) // Changed to nullable
    @Email(message = "Email should be valid")
    private String email;
    
    @ManyToOne(fetch = FetchType.EAGER) // Changed from enum to entity reference
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedDate;
    
    // Other existing relationships...
    
    @Transient
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }
    
    @Transient
    public String getRoleName() {
        return role != null ? role.getName() : null;
    }
}
```

### Database Schema Changes
```sql
-- Create new roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default roles
INSERT INTO roles (name, description, permissions) VALUES 
('ADMIN', 'Administrator with full access', 'ALL'),
('STAFF', 'Staff member with limited admin access', 'MANAGE_ORDERS,MANAGE_CUSTOMERS'),
('CUSTOMER', 'Regular customer', 'SHOP,VIEW_ORDERS'),
('GUEST', 'Guest user with limited access', 'VIEW_PRODUCTS');

-- Add role_id column to users table
ALTER TABLE users ADD COLUMN role_id BIGINT;

-- Update existing users with appropriate role_id based on current role enum
UPDATE users SET role_id = (SELECT id FROM roles WHERE name = users.role);

-- Make role_id NOT NULL and add foreign key constraint
ALTER TABLE users MODIFY COLUMN role_id BIGINT NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id);

-- Update existing email column to allow NULL
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Drop old role enum column (after migration is complete)
-- ALTER TABLE users DROP COLUMN role;

-- Add index for email lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After reviewing all properties identified in the prework, I've identified several areas where properties can be consolidated:

**Redundancy Analysis:**
- Properties related to email validation (4.1, 4.2, 4.3) can be combined into comprehensive validation properties
- Properties about users without email performing operations (3.1, 3.2, 3.3, 3.4) can be consolidated into core functionality properties
- Role migration properties (7.1, 7.2, 7.3) can be combined into migration integrity properties

**Consolidated Properties:**

Property 1: Optional email registration success
*For any* valid username and password combination, registration should succeed regardless of whether email is provided or not
**Validates: Requirements 1.2, 1.3**

Property 2: Email validation consistency
*For any* email input, validation should accept null/empty values as valid and reject invalid formats when non-empty
**Validates: Requirements 1.4, 4.1, 4.2, 4.3**

Property 3: Core functionality with optional email
*For any* user without email, all core shopping operations (cart, checkout, orders) should function normally
**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

Property 4: Role entity relationship integrity
*For any* user-role assignment, the system should maintain referential integrity and allow multiple users to share the same role
**Validates: Requirements 5.2, 5.5, 6.2, 6.5**

Property 5: Role migration preservation
*For any* existing user with a role, migration should preserve the role assignment in the new Role entity structure
**Validates: Requirements 7.1, 7.2, 7.4**

Property 6: Database schema flexibility
*For any* role management operation (create, update, delete), the system should handle changes without affecting user table structure
**Validates: Requirements 5.1, 5.3, 5.4, 6.1**

## Error Handling

### Email Validation Errors
- Invalid email format: Clear validation message with format requirements
- Email already exists: Informative message about duplicate email (if uniqueness is required)
- Email service unavailable: Graceful degradation for email-dependent features

### Role Management Errors
- Invalid role assignment: Validation error with available roles list
- Role deletion with active users: Prevent deletion or provide reassignment options
- Migration failures: Rollback mechanism with detailed error logging

### Database Constraint Errors
- Foreign key violations: Clear error messages about role dependencies
- Null constraint violations: Validation at application level before database operations
- Unique constraint violations: User-friendly messages for duplicate usernames/emails

## Testing Strategy

**Dual testing approach requirements**:

The system MUST use both unit testing and property-based testing approaches:
- Unit tests verify specific examples, edge cases, and error conditions
- Property tests verify universal properties that should hold across all inputs
- Together they provide comprehensive coverage: unit tests catch concrete bugs, property tests verify general correctness

**Property-based testing requirements**:
- Use JUnit 5 with jqwik library for property-based testing in Java
- Configure each property-based test to run a minimum of 100 iterations
- Tag each property-based test with comments referencing the design document property
- Use format: '**Feature: optional-email-registration, Property {number}: {property_text}**'
- Each correctness property MUST be implemented by a SINGLE property-based test

**Unit testing requirements**:
- Unit tests cover specific examples that demonstrate correct behavior
- Integration points between User and Role entities
- Database migration scenarios
- Email validation edge cases
- Role assignment and permission checking

**Test Data Generation**:
- Generate random valid usernames, passwords, and email addresses
- Generate invalid email formats for validation testing
- Create test roles with various permission combinations
- Generate user-role assignment scenarios for relationship testing