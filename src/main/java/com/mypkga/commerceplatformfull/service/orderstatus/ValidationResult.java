package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of order status transition validation.
 * Contains validation outcome and additional information for error handling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    /**
     * Whether the transition is valid
     */
    private boolean valid;
    
    /**
     * Error message if validation failed
     */
    private String errorMessage;
    
    /**
     * List of valid transitions from the current state
     */
    private List<OrderStatus> validTransitions;
    
    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null, null);
    }
    
    /**
     * Create a failed validation result with error message and valid transitions
     */
    public static ValidationResult failure(String errorMessage, List<OrderStatus> validTransitions) {
        return new ValidationResult(false, errorMessage, validTransitions);
    }
    
    /**
     * Create a failed validation result with only error message
     */
    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage, null);
    }
}