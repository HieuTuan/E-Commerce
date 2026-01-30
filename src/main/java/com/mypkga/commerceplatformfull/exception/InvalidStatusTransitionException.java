package com.mypkga.commerceplatformfull.exception;

import com.mypkga.commerceplatformfull.entity.OrderStatus;

/**
 * Exception thrown when an invalid order status transition is attempted.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    
    private final OrderStatus currentStatus;
    private final OrderStatus newStatus;
    
    public InvalidStatusTransitionException(OrderStatus currentStatus, OrderStatus newStatus) {
        super(String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        this.currentStatus = currentStatus;
        this.newStatus = newStatus;
    }
    
    public InvalidStatusTransitionException(OrderStatus currentStatus, OrderStatus newStatus, String message) {
        super(message);
        this.currentStatus = currentStatus;
        this.newStatus = newStatus;
    }
    
    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public OrderStatus getNewStatus() {
        return newStatus;
    }
}