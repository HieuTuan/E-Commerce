# Implementation Plan

- [x] 1. Add new methods to OrderService interface and implementation





  - Add getOrderByIdWithOwnershipCheck method to OrderService interface
  - Add orderHasReturnRequest method to OrderService interface
  - Implement getOrderByIdWithOwnershipCheck in OrderServiceImpl with ownership verification
  - Implement orderHasReturnRequest in OrderServiceImpl
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 2. Refactor ReturnWebController to use OrderService





  - Remove OrderRepository injection from ReturnWebController
  - Add OrderService injection to ReturnWebController (if not already present)
  - Update showReturnRequestForm method to use orderService.getOrderByIdWithOwnershipCheck()
  - Remove manual ownership verification logic (now in service)
  - Update order.hasReturnRequest() calls to use orderService.orderHasReturnRequest()
  - Remove unused OrderRepository import
  - _Requirements: 1.2, 3.1, 3.2, 3.4_


- [x] 3. Refactor ReturnWebController to use PostOfficeService




  - Remove PostOfficeRepository injection from ReturnWebController
  - Add PostOfficeService injection to ReturnWebController
  - Update showReturnRequestForm method to use postOfficeService.getActivePostOffices()
  - Remove unused PostOfficeRepository import
  - _Requirements: 1.1, 3.1, 3.3_

- [ ]* 4. Write unit tests for OrderService new methods
  - Test getOrderByIdWithOwnershipCheck with valid ownership
  - Test getOrderByIdWithOwnershipCheck throws exception when order not found
  - Test getOrderByIdWithOwnershipCheck throws exception when access denied
  - Test orderHasReturnRequest returns correct boolean value
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ]* 5. Write property test for service layer encapsulation
  - **Property 1: Service layer encapsulation**
  - **Validates: Requirements 1.3, 3.5**
  - Use reflection to verify ReturnWebController has no Repository field injections
  - Verify all injected fields are Service classes

- [ ]* 6. Write property test for ownership verification
  - **Property 5: Ownership verification**
  - **Validates: Requirements 2.3**
  - Generate random user IDs and order IDs
  - Verify ownership check correctly allows/denies access

- [ ] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
