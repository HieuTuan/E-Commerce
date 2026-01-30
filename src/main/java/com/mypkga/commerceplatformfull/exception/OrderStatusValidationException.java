package com.mypkga.commerceplatformfull.exception;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when order status validation fails.
 * 
 * This exception provides detailed information about validation failures,
 * including the current status, attempted status, and valid alternatives.
 * 
 * Requirements: 2.2, 4.4
 */
@Getter
public class OrderStatusValidationException extends RuntimeException {
    
    private final Long orderId;
    private final OrderStatus currentStatus;
    private final OrderStatus attemptedStatus;
    private final List<OrderStatus> validTransitions;
    private final String validationErrorCode;
    
    public OrderStatusValidationException(Long orderId, OrderStatus currentStatus, 
                                        OrderStatus attemptedStatus, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
        this.validTransitions = null;
        this.validationErrorCode = "INVALID_TRANSITION";
    }
    
    public OrderStatusValidationException(Long orderId, OrderStatus currentStatus, 
                                        OrderStatus attemptedStatus, List<OrderStatus> validTransitions, 
                                        String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
        this.validTransitions = validTransitions;
        this.validationErrorCode = "INVALID_TRANSITION";
    }
    
    public OrderStatusValidationException(Long orderId, OrderStatus currentStatus, 
                                        OrderStatus attemptedStatus, List<OrderStatus> validTransitions, 
                                        String message, String validationErrorCode) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
        this.validTransitions = validTransitions;
        this.validationErrorCode = validationErrorCode;
    }
    
    /**
     * Create exception for final state modification attempts
     */
    public static OrderStatusValidationException finalStateModification(Long orderId, OrderStatus currentStatus, 
                                                                       OrderStatus attemptedStatus) {
        return new OrderStatusValidationException(
            orderId, 
            currentStatus, 
            attemptedStatus, 
            null,
            String.format("Cannot modify order %d status from final state %s to %s. Final states cannot be changed.", 
                orderId, currentStatus.getDisplayName(), attemptedStatus.getDisplayName()),
            "FINAL_STATE_MODIFICATION"
        );
    }
    
    /**
     * Create exception for invalid transitions with valid alternatives
     */
    public static OrderStatusValidationException invalidTransition(Long orderId, OrderStatus currentStatus, 
                                                                 OrderStatus attemptedStatus, 
                                                                 List<OrderStatus> validTransitions) {
        String validOptions = validTransitions.isEmpty() ? "none" : 
            validTransitions.stream()
                .map(OrderStatus::getDisplayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
                
        return new OrderStatusValidationException(
            orderId, 
            currentStatus, 
            attemptedStatus, 
            validTransitions,
            String.format("Cannot transition order %d from %s to %s. Valid transitions: %s", 
                orderId, currentStatus.getDisplayName(), attemptedStatus.getDisplayName(), validOptions),
            "INVALID_TRANSITION"
        );
    }
    
    /**
     * Create exception for order not found scenarios
     */
    public static OrderStatusValidationException orderNotFound(Long orderId) {
        return new OrderStatusValidationException(
            orderId, 
            null, 
            null, 
            null,
            String.format("Order with ID %d not found", orderId),
            "ORDER_NOT_FOUND"
        );
    }
}