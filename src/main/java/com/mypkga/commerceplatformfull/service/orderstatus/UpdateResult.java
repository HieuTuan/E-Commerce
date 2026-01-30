package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an order status update operation.
 * Contains information about the success/failure and status changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResult {
    
    /**
     * Whether the update was successful
     */
    private boolean success;
    
    /**
     * The previous status before update
     */
    private OrderStatus previousStatus;
    
    /**
     * The new status after update
     */
    private OrderStatus newStatus;
    
    /**
     * Error message if update failed
     */
    private String errorMessage;
    
    /**
     * Create a successful update result
     */
    public static UpdateResult success(OrderStatus previousStatus, OrderStatus newStatus) {
        return new UpdateResult(true, previousStatus, newStatus, null);
    }
    
    /**
     * Create a failed update result
     */
    public static UpdateResult failure(String errorMessage) {
        return new UpdateResult(false, null, null, errorMessage);
    }
    
    /**
     * Create a failed update result with status information
     */
    public static UpdateResult failure(OrderStatus currentStatus, String errorMessage) {
        return new UpdateResult(false, currentStatus, null, errorMessage);
    }
}