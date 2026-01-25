# Design Document

## Overview

Thiết kế này mô tả hai cải tiến quan trọng cho hệ thống quản lý người dùng:

1. **Tách bảng Role riêng biệt**: Tách role thành một entity riêng với mối quan hệ 1-n với User để quản lý quyền hạn linh hoạt và có thể mở rộng
2. **Loại bỏ email hoàn toàn**: Xóa bỏ tất cả chức năng liên quan đến email để đơn giản hóa hệ thống

Các thay đổi này sẽ duy trì tính toàn vẹn dữ liệu và khả năng tương thích ngược trong khi cải thiện kiến trúc hệ thống.

## Architecture

Kiến trúc được cập nhật với bảng Role mới và loại bỏ email:

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
│ permissions     │         │ full_name       │
│ created_date    │         │ phone           │
│ updated_date    │         │ address         │
└─────────────────┘         │ role_id (FK)    │
                            │ created_date    │
                            │ updated_date    │
                            └─────────────────┘
```

## Components and Interfaces

### 1. Role Entity (New)
- Tạo entity Role mới với các trường: id, name, description, permissions
- Thiết lập mối quan hệ 1-n với User entity
- Implement validation và business logic cho role management

### 2. User Entity Updates
- Loại bỏ hoàn toàn trường email và tất cả logic liên quan
- Thay thế enum role bằng foreign key reference đến Role entity
- Giữ lại các trường: username, password, fullName, phone, address
- Duy trì backward compatibility với dữ liệu hiện tại

### 3. Registration Controller
- Cập nhật để chỉ xử lý username và password
- Loại bỏ tất cả logic xử lý email
- Cập nhật logic gán role mặc định cho user mới
- Duy trì existing endpoints và behavior

### 4. Registration Form (HTML)
- Loại bỏ hoàn toàn trường email
- Chỉ hiển thị Username và Password là bắt buộc
- Cập nhật client-side validation

### 5. User Service Layer
- Loại bỏ tất cả business logic liên quan đến email
- Thêm methods để quản lý role assignments
- Cập nhật notification logic để không sử dụng email

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
    
    @Column(name = "full_name", length = 100)
    private String fullName;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
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

-- Drop email column completely
ALTER TABLE users DROP COLUMN email;

-- Drop old role enum column (after migration is complete)
-- ALTER TABLE users DROP COLUMN role;

-- Add index for role lookups
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*
### Property Reflection

After reviewing all properties identified in the prework, I've identified several areas where properties can be consolidated:

**Redundancy Analysis:**
- Properties related to role entity operations (1.1, 1.2, 1.3, 1.5, 2.2, 2.5) can be combined into role management properties
- Properties about migration and data preservation (2.4, 3.1) can be consolidated
- Properties about email removal (5.1, 5.2, 5.5) can be combined into email elimination properties

**Consolidated Properties:**

Property 1: Role entity management integrity
*For any* role management operation (create, update, assign, delete), the system should maintain proper database relationships and referential integrity
**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.5**

Property 2: Migration data preservation
*For any* existing user with a role assignment, migration should preserve the role relationship in the new Role entity structure
**Validates: Requirements 2.4, 3.1**

Property 3: Registration without email
*For any* valid username and password combination, registration should succeed and assign a default role without requiring email information
**Validates: Requirements 4.2, 4.4, 4.5**

Property 4: Security integration with role entities
*For any* security check or access control operation, the system should work correctly with the new Role entity structure
**Validates: Requirements 3.3, 3.4**

Property 5: Email elimination completeness
*For any* system operation, no email-related data should be requested, stored, or processed
**Validates: Requirements 5.2, 5.3, 5.4, 5.5**

Property 6: Input validation without email
*For any* user input, validation should work correctly without any email-related validation logic
**Validates: Requirements 4.3, 5.5**

## Error Handling

### Role Management Errors
- Invalid role assignment: Validation error with available roles list
- Role deletion with active users: Prevent deletion or provide reassignment options
- Migration failures: Rollback mechanism with detailed error logging
- Duplicate role names: Clear validation messages about uniqueness constraints

### Registration Errors
- Invalid username format: Clear validation message with format requirements
- Username already exists: Informative message about duplicate usernames
- Password validation: Clear requirements for password strength
- Missing required fields: Specific messages about required username/password

### Database Constraint Errors
- Foreign key violations: Clear error messages about role dependencies
- Null constraint violations: Validation at application level before database operations
- Unique constraint violations: User-friendly messages for duplicate usernames

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
- Use format: '**Feature: role-table-separation, Property {number}: {property_text}**'
- Each correctness property MUST be implemented by a SINGLE property-based test

**Unit testing requirements**:
- Unit tests cover specific examples that demonstrate correct behavior
- Integration points between User and Role entities
- Database migration scenarios
- Registration form validation
- Role assignment and permission checking

**Test Data Generation**:
- Generate random valid usernames and passwords
- Generate invalid input formats for validation testing
- Create test roles with various permission combinations
- Generate user-role assignment scenarios for relationship testing
- Test migration scenarios with existing enum role data