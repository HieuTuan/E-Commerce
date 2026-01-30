package com.mypkga.commerceplatformfull.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer delivery confirmation responses.
 * 
 * This DTO provides a standardized format for successful customer confirmation responses,
 * including information about the status change and confirmation details.
 * 
 * Requirements: 1.5, 4.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerConfirmationResponse {
    
    /**
     * Indicates if the operation was successful
     */
    private boolean success;
    
    /**
     * The ID of the order that was confirmed
     */
    private Long orderId;
    
    /**
     * The previous status before confirmation (should be DELIVERED)
     */
    private String previousStatus;
    
    /**
     * Display name of the previous status
     */
    private String previousStatusDisplayName;
    
    /**
     * The new status after confirmation (CONFIRMED_BY_CUSTOMER)
     */
    private String newStatus;
    
    /**
     * Display name of the new status
     */
    private String newStatusDisplayName;
    
    /**
     * Customer who confirmed the delivery
     */
    private String confirmedBy;
    
    /**
     * Timestamp when the confirmation occurred
     */
    private String timestamp;
    
    /**
     * Human-readable message describing the confirmation
     */
    private String message;
    
    /**
     * Optional notes provided by the customer
     */
    private String notes;
    
    /**
     * Optional delivery rating provided by the customer
     */
    private Integer deliveryRating;
    
    /**
     * Create a successful confirmation response
     */
    public static CustomerConfirmationResponse success(Long orderId, OrderStatus previousStatus, 
                                                     OrderStatus newStatus, String confirmedBy, 
                                                     String timestamp, String notes, Integer deliveryRating) {
        String message = String.format("Delivery confirmed successfully. Order status updated from %s to %s", 
            previousStatus != null ? previousStatus.getDisplayName() : "unknown",
            newStatus.getDisplayName());
            
        return CustomerConfirmationResponse.builder()
            .success(true)
            .orderId(orderId)
            .previousStatus(previousStatus != null ? previousStatus.name() : null)
            .previousStatusDisplayName(previousStatus != null ? previousStatus.getDisplayName() : null)
            .newStatus(newStatus.name())
            .newStatusDisplayName(newStatus.getDisplayName())
            .confirmedBy(confirmedBy)
            .timestamp(timestamp)
            .message(message)
            .notes(notes)
            .deliveryRating(deliveryRating)
            .build();
    }
}