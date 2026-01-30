package com.mypkga.commerceplatformfull.dto;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.validation.ValidOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for order status update requests with enhanced validation.
 * 
 * This DTO is used for API requests to update order status and includes
 * comprehensive validation to ensure data integrity and proper error handling.
 * 
 * Requirements: 2.2, 4.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidOrderStatus
public class StatusUpdateRequest {
    
    /**
     * The new status to set for the order.
     * Must be a valid OrderStatus enum value.
     */
    @NotNull(message = "Status is required and cannot be null")
    private OrderStatus status;
    
    /**
     * Optional notes or reason for the status change.
     * Limited to 500 characters to prevent excessive data.
     */
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    /**
     * Optional reason code for the status change.
     * Used for categorizing different types of status changes.
     */
    @Size(max = 50, message = "Reason code cannot exceed 50 characters")
    private String reasonCode;
    
    /**
     * Flag indicating if this is a forced update (admin only).
     * Used for emergency status corrections.
     */
    private boolean forceUpdate = false;
}