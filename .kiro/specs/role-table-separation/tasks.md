# Implementation Plan

- [x] 1. Create Role entity and repository infrastructure


  - Create Role entity with proper JPA annotations and relationships
  - Create RoleRepository interface with necessary query methods
  - Create RoleService for business logic operations
  - Set up basic CRUD operations for role management
  - _Requirements: 1.1, 2.1, 2.2_

- [x] 1.1 Write property test for role entity management


  - **Property 1: Role entity management integrity**


  - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.5**

- [ ] 2. Update User entity to remove email and add role relationship
  - Remove email field and all related annotations completely
  - Replace enum role with foreign key reference to Role entity
  - Update existing relationships and constraints
  - Add helper methods for role checking
  - _Requirements: 2.2, 5.1, 5.5_




- [x] 4. Update UserService and related services


  - Modify UserService to work with Role entities
  - Update user creation logic to assign default role
  - Remove all email-related business logic completely
  - Modify authentication and authorization logic
  - _Requirements: 1.2, 4.4, 5.2, 5.5_


- [x] 5. Update registration controller and forms


  - Remove all email-related code from registration controller
  - Update validation logic to only handle username and password
  - Modify registration form HTML to remove email field completely
  - Update client-side validation JavaScript
  - _Requirements: 4.1, 4.2, 4.3, 5.2_


- [x] 6. Update security configuration


  - Modify SecurityConfig to work with new Role entity structure
  - Update role-based access control annotations
  - Ensure proper authentication with new role system
  - Test security constraints with role changes
  - _Requirements: 3.3, 3.4_


- [ ] 7. Update user profile and management interfaces
  - Remove all email-related fields from user profile templates
  - Update admin user management to work with Role entities
  - Add role management interface for administrators
  - Remove email display logic completely
  - _Requirements: 1.3, 1.4, 5.1, 5.2_


- [x] 8. Update DataInitializer for new role structure



  - Modify DataInitializer to create default roles
  - Update user creation to use Role entities
  - Remove all email-related initialization code
  - Ensure proper role assignments for test users
  - _Requirements: 2.4, 4.4_

- [x] 9. Update notification and order services


  - Remove all email notification services completely
  - Implement in-app notification system as alternative
  - Update order confirmation logic to not use email
  - Remove all email-dependent features
  - _Requirements: 5.3, 5.4_

- [x] 10. Checkpoint - Ensure all tests pass


  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Update templates and UI components






  - Update all user-related templates to work with Role entities
  - Remove all email-related form fields and displays
  - Update role display in user interfaces
  - Ensure no email references remain in any template
  - _Requirements: 5.1, 5.2_



- [ ] 12. Final cleanup and validation


  - Remove all unused email-related code and imports
  - Verify no email references remain in codebase
  - Test role-based access control thoroughly
  - Validate migration scripts work correctly
  - _Requirements: All_
