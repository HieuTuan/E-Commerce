package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;

/**
 * Interface for managing order status updates with validation.
 * This service coordinates status changes with proper validation and auditing.
 */
public interface OrderStatusManager {
    
    /**
     * Update order status with validation
     * 
     * @param orderId The ID of the order to update
     * @param newStatus The new status to set
     * @param userId The ID of the user making the change
     * @return UpdateResult containing the outcome of the operation
     */
    UpdateResult updateOrderStatus(Long orderId, OrderStatus newStatus, String userId);
    
    /**
     * Get the current status of an order
     * 
     * @param orderId The ID of the order
     * @return The current OrderStatus
     * @throws IllegalArgumentException if order not found
     */
    OrderStatus getCurrentStatus(Long orderId);
    
    /**
     * Confirm delivery by customer and update order status to CONFIRMED_BY_CUSTOMER
     * 
     * @param orderId The ID of the order to confirm
     * @param customerId The ID of the customer confirming delivery
     * @param notes Optional notes from the customer
     * @return UpdateResult containing the outcome of the operation
     * @throws IllegalArgumentException if order not found or customer not authorized
     * @throws IllegalStateException if order is not in DELIVERED status
     */
    UpdateResult confirmDeliveryByCustomer(Long orderId, Long customerId, String notes);
}