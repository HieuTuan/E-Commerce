package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import com.mypkga.commerceplatformfull.service.AuditLogService;
import com.mypkga.commerceplatformfull.service.OrderTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of OrderStatusManager that provides order status updates with validation.
 * This service coordinates status changes with proper validation and auditing.
 * 
 * Requirements: 2.4, 4.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusManagerImpl implements OrderStatusManager {
    
    private final StateTransitionValidator stateTransitionValidator;
    private final OrderRepository orderRepository;
    private final OrderTimelineService orderTimelineService;
    private final AuditLogService auditLogService;
    
    /**
     * Update order status with validation
     * 
     * @param orderId The ID of the order to update
     * @param newStatus The new status to set
     * @param userId The ID of the user making the change
     * @return UpdateResult containing the outcome of the operation
     */
    @Override
    @Transactional
    public UpdateResult updateOrderStatus(Long orderId, OrderStatus newStatus, String userId) {
        try {
            // Validate input parameters
            if (orderId == null) {
                log.warn("Order status update attempted with null orderId by user: {}", userId);
                return UpdateResult.failure("Order ID cannot be null");
            }
            
            if (newStatus == null) {
                log.warn("Order status update attempted with null newStatus for order {} by user: {}", orderId, userId);
                return UpdateResult.failure("New status cannot be null");
            }
            
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("Order status update attempted with null/empty userId for order {}", orderId);
                return UpdateResult.failure("User ID cannot be null or empty");
            }
            
            // Get current order
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("Order status update attempted for non-existent order {} by user: {}", orderId, userId);
                return UpdateResult.failure("Order not found with ID: " + orderId);
            }
            
            OrderStatus currentStatus = order.getCurrentStatus();
            
            // Check if trying to update to the same status
            if (currentStatus == newStatus) {
                log.debug("Order {} already has status {}, no update needed", orderId, newStatus);
                return UpdateResult.success(currentStatus, newStatus);
            }
            
            // Validate transition using StateTransitionValidator
            ValidationResult validationResult = stateTransitionValidator.validateTransition(currentStatus, newStatus);
            
            if (!validationResult.isValid()) {
                log.warn("Invalid status transition attempted for order {}: {} -> {} by user: {}. Error: {}", 
                    orderId, currentStatus, newStatus, userId, validationResult.getErrorMessage());
                
                // Log the invalid transition attempt for audit purposes
                auditLogService.logInvalidStatusTransition(orderId, currentStatus, newStatus, userId, 
                    validationResult.getErrorMessage());
                
                return UpdateResult.failure(currentStatus, validationResult.getErrorMessage());
            }
            
            // Validation passed - proceed with status update
            log.info("Updating order {} status from {} to {} by user: {}", 
                orderId, currentStatus, newStatus, userId);
            
            // Update order status
            order.updateCurrentStatus(newStatus);
            Order savedOrder = orderRepository.save(order);
            
            // Create timeline entry
            orderTimelineService.createTimelineEntry(orderId, newStatus, userId, 
                String.format("Status updated from %s to %s", currentStatus.getDisplayName(), newStatus.getDisplayName()));
            
            // Log successful status change for audit purposes
            auditLogService.logOrderStatusChange(orderId, currentStatus, newStatus, userId, 
                "Status update completed successfully");
            
            log.info("Successfully updated order {} status from {} to {} by user: {}", 
                orderId, currentStatus, newStatus, userId);
            
            return UpdateResult.success(currentStatus, newStatus);
            
        } catch (Exception e) {
            log.error("Error updating order {} status to {} by user: {}", orderId, newStatus, userId, e);
            
            // Log the error for audit purposes
            auditLogService.logSystemError("ORDER_STATUS_UPDATE_ERROR", orderId, 
                String.format("Failed to update status to %s: %s", newStatus, e.getMessage()));
            
            return UpdateResult.failure("System error occurred while updating order status: " + e.getMessage());
        }
    }
    
    /**
     * Get the current status of an order
     * 
     * @param orderId The ID of the order
     * @return The current OrderStatus
     * @throws IllegalArgumentException if order not found or invalid input
     */
    @Override
    public OrderStatus getCurrentStatus(Long orderId) {
        try {
            // Validate input parameters
            if (orderId == null) {
                log.warn("getCurrentStatus called with null orderId");
                throw new IllegalArgumentException("Order ID cannot be null");
            }
            
            if (orderId <= 0) {
                log.warn("getCurrentStatus called with invalid orderId: {}", orderId);
                throw new IllegalArgumentException("Order ID must be a positive number");
            }
            
            log.debug("Querying current status for order: {}", orderId);
            
            // Query current status from database
            Order order = orderRepository.findById(orderId).orElse(null);
            
            if (order == null) {
                log.warn("Order not found with ID: {}", orderId);
                throw new IllegalArgumentException("Order not found with ID: " + orderId);
            }
            
            OrderStatus currentStatus = order.getCurrentStatus();
            log.debug("Retrieved current status {} for order: {}", currentStatus, orderId);
            
            return currentStatus;
            
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors as-is
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving current status for order {}: {}", orderId, e.getMessage(), e);
            
            // Log the error for audit purposes
            auditLogService.logSystemError("GET_CURRENT_STATUS_ERROR", orderId, 
                String.format("Failed to retrieve current status: %s", e.getMessage()));
            
            throw new RuntimeException("System error occurred while retrieving order status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Confirm delivery by customer and update order status to CONFIRMED_BY_CUSTOMER
     * 
     * @param orderId The ID of the order to confirm
     * @param customerId The ID of the customer confirming delivery
     * @param notes Optional notes from the customer
     * @return UpdateResult containing the outcome of the operation
     */
    @Override
    @Transactional
    public UpdateResult confirmDeliveryByCustomer(Long orderId, Long customerId, String notes) {
        try {
            // Validate input parameters
            if (orderId == null) {
                log.warn("Customer delivery confirmation attempted with null orderId by customer: {}", customerId);
                return UpdateResult.failure("Order ID cannot be null");
            }
            
            if (customerId == null) {
                log.warn("Customer delivery confirmation attempted with null customerId for order: {}", orderId);
                return UpdateResult.failure("Customer ID cannot be null");
            }
            
            // Get current order
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("Customer delivery confirmation attempted for non-existent order {} by customer: {}", orderId, customerId);
                return UpdateResult.failure("Order not found with ID: " + orderId);
            }
            
            // Check customer authorization - only the customer who placed the order can confirm
            if (!order.getUser().getId().equals(customerId)) {
                log.warn("Unauthorized customer delivery confirmation attempt for order {} by customer: {}. Order belongs to customer: {}", 
                    orderId, customerId, order.getUser().getId());
                return UpdateResult.failure("You are not authorized to confirm delivery for this order");
            }
            
            OrderStatus currentStatus = order.getCurrentStatus();
            
            // Check if order is in DELIVERED status
            if (currentStatus != OrderStatus.DELIVERED) {
                log.warn("Customer delivery confirmation attempted for order {} in invalid status {} by customer: {}", 
                    orderId, currentStatus, customerId);
                return UpdateResult.failure("Order must be in DELIVERED status to confirm delivery. Current status: " + currentStatus.getDisplayName());
            }
            
            // Validate transition using StateTransitionValidator
            OrderStatus newStatus = OrderStatus.CONFIRMED_BY_CUSTOMER;
            ValidationResult validationResult = stateTransitionValidator.validateTransition(currentStatus, newStatus);
            
            if (!validationResult.isValid()) {
                log.warn("Invalid status transition attempted for customer delivery confirmation on order {}: {} -> {} by customer: {}. Error: {}", 
                    orderId, currentStatus, newStatus, customerId, validationResult.getErrorMessage());
                
                // Log the invalid transition attempt for audit purposes
                auditLogService.logInvalidStatusTransition(orderId, currentStatus, newStatus, customerId.toString(), 
                    validationResult.getErrorMessage());
                
                return UpdateResult.failure(currentStatus, validationResult.getErrorMessage());
            }
            
            // Validation passed - proceed with status update
            log.info("Customer {} confirming delivery for order {} - updating status from {} to {}", 
                customerId, orderId, currentStatus, newStatus);
            
            // Update order status
            order.updateCurrentStatus(newStatus);
            Order savedOrder = orderRepository.save(order);
            
            // Create timeline entry with customer notes if provided
            String timelineMessage = "Delivery confirmed by customer";
            if (notes != null && !notes.trim().isEmpty()) {
                timelineMessage += ": " + notes.trim();
            }
            orderTimelineService.createTimelineEntry(orderId, newStatus, "CUSTOMER", timelineMessage);
            
            // Log successful customer confirmation for audit purposes
            auditLogService.logOrderStatusChange(orderId, currentStatus, newStatus, customerId.toString(), 
                "Customer delivery confirmation completed successfully" + (notes != null ? " with notes: " + notes : ""));
            
            log.info("Successfully confirmed delivery for order {} by customer {} - status updated from {} to {}", 
                orderId, customerId, currentStatus, newStatus);
            
            return UpdateResult.success(currentStatus, newStatus);
            
        } catch (Exception e) {
            log.error("Error confirming delivery for order {} by customer: {}", orderId, customerId, e);
            
            // Log the error for audit purposes
            auditLogService.logSystemError("CUSTOMER_DELIVERY_CONFIRMATION_ERROR", orderId, 
                String.format("Failed to confirm delivery by customer %s: %s", customerId, e.getMessage()));
            
            return UpdateResult.failure("System error occurred while confirming delivery: " + e.getMessage());
        }
    }
}