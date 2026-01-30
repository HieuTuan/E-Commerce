package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;

import java.util.List;

/**
 * Interface for validating order status transitions.
 * This is the main component responsible for enforcing business rules
 * around order status changes.
 */
public interface StateTransitionValidator {
    
    /**
     * Validate a transition from current state to new state
     * 
     * @param currentState The current order status
     * @param newState The proposed new order status
     * @return ValidationResult containing validation outcome and details
     */
    ValidationResult validateTransition(OrderStatus currentState, OrderStatus newState);
    
    /**
     * Get all valid next states from the current state
     * 
     * @param currentState The current order status
     * @return List of valid next states
     */
    List<OrderStatus> getValidNextStates(OrderStatus currentState);
    
    /**
     * Check if a transition is valid (simple boolean check)
     * 
     * @param currentState The current order status
     * @param newState The proposed new order status
     * @return true if transition is valid, false otherwise
     */
    boolean isValidTransition(OrderStatus currentState, OrderStatus newState);
}