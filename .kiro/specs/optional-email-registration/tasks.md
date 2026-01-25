# Implementation Plan

- [ ] 1. Create Role entity and repository infrastructure
  - Create Role entity with proper JPA annotations and relationships
  - Create RoleRepository interface with necessary query methods
  - Create RoleService for business logic operations
  - Set up basic CRUD operations for role management
  - _Requirements: 5.1, 6.1, 6.2_

- [ ] 1.1 Write property test for role entity creation
  - **Property 6: Database schema flexibility**
  - **Validates: Requirements 5.1, 6.1**

- [ ] 2. Update User entity for role relationship and optional email
  - Modify User entity to reference Role entity instead of enum
  - Update email field to be nullable with proper validation
  - Add helper methods for role and email checking
  - Update existing relationships and constraints
  - _Requirements: 1.1, 2.1, 5.2, 6.2_

- [ ] 2.1 Write property test for optional email validation
  - **Property 2: Email validation consistency**
  - **Validates: Requirements 1.4, 4.1, 4.2, 4.3**

- [ ] 2.2 Write property test for user-role relationship
  - **Property 4: Role entity relationship integrity**
  - **Validates: Requirements 5.2, 5.5, 6.2, 6.5**

- [ ] 3. Create database migration scripts
  - Create SQL scripts to add roles table with default data
  - Create migration script to add role_id column to users table
  - Create script to migrate existing enum role data to new structure
  - Add proper indexes and foreign key constraints
  - _Requirements: 6.4, 7.1, 7.2_

- [ ] 3.1 Write property test for migration data preservation
  - **Property 5: Role migration preservation**
  - **Validates: Requirements 7.1, 7.2, 7.4**

- [ ] 4. Update UserService and related services
  - Modify UserService to work with Role entities
  - Update user creation logic to assign default role
  - Modify authentication and authorization logic
  - Update email handling for optional email scenarios
  - _Requirements: 2.2, 2.4, 4.5_

- [ ] 4.1 Write property test for optional email registration
  - **Property 1: Optional email registration success**
  - **Validates: Requirements 1.2, 1.3**

- [ ] 5. Update registration controller and forms
  - Modify registration controller to handle optional email
  - Update validation logic for email and role assignment
  - Modify registration form HTML to make email optional
  - Update client-side validation JavaScript
  - _Requirements: 1.1, 1.4, 4.1, 4.2_

- [ ] 5.1 Write unit tests for registration controller
  - Test registration with and without email
  - Test email validation scenarios
  - Test role assignment during registration
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 6. Update security configuration
  - Modify SecurityConfig to work with new Role entity structure
  - Update role-based access control annotations
  - Ensure proper authentication with new role system
  - Test security constraints with role changes
  - _Requirements: 7.3, 7.4_

- [ ] 7. Update user profile and management interfaces
  - Modify user profile templates to handle optional email
  - Update admin user management to work with Role entities
  - Add role management interface for administrators
  - Update user display logic for missing email addresses
  - _Requirements: 2.3, 5.3, 5.4_

- [ ] 7.1 Write property test for core functionality with optional email
  - **Property 3: Core functionality with optional email**
  - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**

- [ ] 8. Update DataInitializer for new role structure
  - Modify DataInitializer to create default roles
  - Update user creation to use Role entities
  - Ensure proper role assignments for test users
  - Maintain backward compatibility during development
  - _Requirements: 6.4, 7.1_

- [ ] 9. Update notification and email services
  - Modify email notification services to handle users without email
  - Implement alternative notification methods for users without email
  - Update order confirmation logic for optional email
  - Add graceful handling for email-dependent features
  - _Requirements: 2.2, 3.5_

- [ ] 10. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Update templates and UI components
  - Update all user-related templates to work with Role entities
  - Modify forms to handle optional email display
  - Update role display in user interfaces
  - Ensure consistent error messaging across all forms
  - _Requirements: 2.3, 4.5_

- [ ] 11.1 Write integration tests for UI components
  - Test registration form with optional email
  - Test user profile display with missing email
  - Test role-based UI element visibility
  - _Requirements: 1.1, 2.3, 4.5_

- [ ] 12. Final testing and validation
  - Run comprehensive tests on all affected functionality
  - Verify migration scripts work correctly
  - Test role-based access control thoroughly
  - Validate email optional functionality end-to-end
  - _Requirements: All_

- [ ] 13. Final Checkpoint - Make sure all tests are passing
  - Ensure all tests pass, ask the user if questions arise.