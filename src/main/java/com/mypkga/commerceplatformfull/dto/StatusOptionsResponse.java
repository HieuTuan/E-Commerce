package com.mypkga.commerceplatformfull.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mypkga.commerceplatformfull.service.orderstatus.StatusOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for status options responses.
 * 
 * This DTO provides a standardized format for status options responses,
 * including available status transitions and customer confirmation options.
 * 
 * Requirements: 4.1, 4.2, 4.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusOptionsResponse {
    
    /**
     * The ID of the order
     */
    private Long orderId;
    
    /**
     * Whether the order status can be changed
     */
    private boolean canChangeStatus;
    
    /**
     * List of available status options for staff/admin
     */
    private List<StatusOption> statusOptions;
    
    /**
     * Whether customer delivery confirmation is required
     */
    private boolean requiresCustomerConfirmation;
    
    /**
     * Customer delivery confirmation option (if applicable)
     */
    private StatusOption customerConfirmationOption;
    
    /**
     * Timestamp when the response was generated
     */
    private String timestamp;
    
    /**
     * Additional metadata about the current order state
     */
    private OrderStatusMetadata metadata;
    
    /**
     * Metadata about the current order status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderStatusMetadata {
        
        /**
         * Current status of the order
         */
        private String currentStatus;
        
        /**
         * Display name of the current status
         */
        private String currentStatusDisplayName;
        
        /**
         * Whether the current status is a final state
         */
        private boolean isFinalState;
        
        /**
         * Number of available transitions
         */
        private int availableTransitionsCount;
        
        /**
         * Whether the order is in a customer-actionable state
         */
        private boolean customerActionable;
    }
}