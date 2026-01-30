package com.mypkga.commerceplatformfull.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for order status update responses.
 * 
 * This DTO provides a standardized format for successful status update responses,
 * including comprehensive information about the status change and metadata.
 * 
 * Requirements: 2.2, 4.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusUpdateResponse {
    
    /**
     * Indicates if the operation was successful
     */
    private boolean success;
    
    /**
     * The ID of the order that was updated
     */
    private Long orderId;
    
    /**
     * The previous status before the update
     */
    private String previousStatus;
    
    /**
     * Display name of the previous status
     */
    private String previousStatusDisplayName;
    
    /**
     * The new status after the update
     */
    private String newStatus;
    
    /**
     * Display name of the new status
     */
    private String newStatusDisplayName;
    
    /**
     * User who performed the update
     */
    private String updatedBy;
    
    /**
     * Timestamp when the update occurred
     */
    private String timestamp;
    
    /**
     * Human-readable message describing the update
     */
    private String message;
    
    /**
     * Optional notes provided with the update
     */
    private String notes;
    
    /**
     * Optional reason code for the update
     */
    private String reasonCode;
    
    /**
     * Indicates if this was a forced update
     */
    private Boolean forceUpdate;
    
    /**
     * Create a successful response
     */
    public static StatusUpdateResponse success(Long orderId, OrderStatus previousStatus, 
                                             OrderStatus newStatus, String updatedBy, 
                                             String timestamp, String notes, String reasonCode, 
                                             Boolean forceUpdate) {
        String message = String.format("Order status updated successfully from %s to %s", 
            previousStatus != null ? previousStatus.getDisplayName() : "unknown",
            newStatus.getDisplayName());
            
        return StatusUpdateResponse.builder()
            .success(true)
            .orderId(orderId)
            .previousStatus(previousStatus != null ? previousStatus.name() : null)
            .previousStatusDisplayName(previousStatus != null ? previousStatus.getDisplayName() : null)
            .newStatus(newStatus.name())
            .newStatusDisplayName(newStatus.getDisplayName())
            .updatedBy(updatedBy)
            .timestamp(timestamp)
            .message(message)
            .notes(notes)
            .reasonCode(reasonCode)
            .forceUpdate(forceUpdate)
            .build();
    }
}