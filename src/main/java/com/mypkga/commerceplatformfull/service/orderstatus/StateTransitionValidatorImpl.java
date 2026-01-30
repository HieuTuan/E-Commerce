package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of StateTransitionValidator.
 * Validates order status transitions based on configured rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateTransitionValidatorImpl implements StateTransitionValidator {
    
    private final StateTransitionRules stateTransitionRules;
    
    @Override
    public ValidationResult validateTransition(OrderStatus currentState, OrderStatus newState) {
        // Validate input parameters
        if (currentState == null) {
            return ValidationResult.failure("Current state cannot be null");
        }
        
        if (newState == null) {
            return ValidationResult.failure("New state cannot be null");
        }
        
        // Check if transition is valid according to rules
        if (!stateTransitionRules.isValidTransition(currentState, newState)) {
            List<OrderStatus> validTransitions = stateTransitionRules.getValidNextStates(currentState);
            String errorMessage = String.format(
                "Invalid transition from %s to %s. Valid transitions are: %s",
                currentState.getDisplayName(),
                newState.getDisplayName(),
                validTransitions.isEmpty() ? "none (final state)" : 
                    validTransitions.stream()
                        .map(OrderStatus::getDisplayName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("none")
            );
            
            log.warn("Invalid status transition attempted: {} -> {}", currentState, newState);
            return ValidationResult.failure(errorMessage, validTransitions);
        }
        
        log.debug("Valid status transition: {} -> {}", currentState, newState);
        return ValidationResult.success();
    }
    
    @Override
    public List<OrderStatus> getValidNextStates(OrderStatus currentState) {
        if (currentState == null) {
            return List.of();
        }
        
        return stateTransitionRules.getValidNextStates(currentState);
    }
    
    @Override
    public boolean isValidTransition(OrderStatus currentState, OrderStatus newState) {
        if (currentState == null || newState == null) {
            return false;
        }
        
        return stateTransitionRules.isValidTransition(currentState, newState);
    }
}