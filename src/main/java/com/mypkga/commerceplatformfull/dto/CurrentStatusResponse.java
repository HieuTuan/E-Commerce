package com.mypkga.commerceplatformfull.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for current status query responses.
 * 
 * This DTO provides a standardized format for current status responses,
 * including status information and metadata.
 * 
 * Requirements: 4.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentStatusResponse {
    
    /**
     * The ID of the order
     */
    private Long orderId;
    
    /**
     * Current status of the order
     */
    private String status;
    
    /**
     * Display name of the current status
     */
    private String displayName;
    
    /**
     * Whether the current status is a final state
     */
    private boolean isFinalState;
    
    /**
     * Timestamp when the response was generated
     */
    private String timestamp;
    
    /**
     * Additional status metadata
     */
    private StatusMetadata metadata;
    
    /**
     * Create response from OrderStatus
     */
    public static CurrentStatusResponse from(Long orderId, OrderStatus status, String timestamp) {
        StatusMetadata metadata = StatusMetadata.builder()
            .canTransition(!status.isFinalState())
            .statusCategory(determineStatusCategory(status))
            .nextStepsAvailable(status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED)
            .build();
            
        return CurrentStatusResponse.builder()
            .orderId(orderId)
            .status(status.name())
            .displayName(status.getDisplayName())
            .isFinalState(status.isFinalState())
            .timestamp(timestamp)
            .metadata(metadata)
            .build();
    }
    
    /**
     * Determine status category for UI purposes
     */
    private static String determineStatusCategory(OrderStatus status) {
        switch (status) {
            case PENDING:
                return "awaiting_confirmation";
            case CONFIRMED:
                return "confirmed";
            case SHIPPING:
                return "in_transit";
            case AWAITING_CONFIRMATION:
                return "awaiting_delivery_confirmation";
            case DELIVERED:
                return "delivered";
            case CONFIRMED_BY_CUSTOMER:
                return "completed";
            case CANCELLED:
                return "cancelled";
            default:
                return "unknown";
        }
    }
    
    /**
     * Metadata about the status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusMetadata {
        
        /**
         * Whether the status can transition to another state
         */
        private boolean canTransition;
        
        /**
         * Category of the status for UI grouping
         */
        private String statusCategory;
        
        /**
         * Whether there are next steps available
         */
        private boolean nextStepsAvailable;
    }
}