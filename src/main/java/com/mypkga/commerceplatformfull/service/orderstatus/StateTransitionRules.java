package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration class that defines all valid order status transitions.
 * This is the single source of truth for transition rules.
 */
@Component
public class StateTransitionRules {
    
    private final Map<OrderStatus, List<OrderStatus>> transitionRules;
    
    public StateTransitionRules() {
        // Initialize transition rules based on requirements
        this.transitionRules = Map.of(
            // PENDING can go to CONFIRMED or CANCELLED
            OrderStatus.PENDING, Arrays.asList(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            
            // CONFIRMED can go to SHIPPING or CANCELLED  
            OrderStatus.CONFIRMED, Arrays.asList(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
            
            // SHIPPING can go directly to DELIVERED or CANCELLED (removed AWAITING_CONFIRMATION)
            OrderStatus.SHIPPING, Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            
            // AWAITING_CONFIRMATION can go to DELIVERED or CANCELLED (kept for existing orders)
            OrderStatus.AWAITING_CONFIRMATION, Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            
            // DELIVERED can go to CONFIRMED_BY_CUSTOMER (customer confirms receipt)
            OrderStatus.DELIVERED, Arrays.asList(OrderStatus.CONFIRMED_BY_CUSTOMER),
            
            // CONFIRMED_BY_CUSTOMER is final state - no transitions allowed
            OrderStatus.CONFIRMED_BY_CUSTOMER, Collections.emptyList(),
            
            // CANCELLED is final state - no transitions allowed
            OrderStatus.CANCELLED, Collections.emptyList()
        );
    }
    
    /**
     * Get valid next states for a given current state
     */
    public List<OrderStatus> getValidNextStates(OrderStatus currentState) {
        return transitionRules.getOrDefault(currentState, Collections.emptyList());
    }
    
    /**
     * Check if a transition is valid
     */
    public boolean isValidTransition(OrderStatus fromState, OrderStatus toState) {
        List<OrderStatus> validStates = getValidNextStates(fromState);
        return validStates.contains(toState);
    }
    
    /**
     * Get all transition rules as a map
     */
    public Map<OrderStatus, List<OrderStatus>> getAllRules() {
        return Collections.unmodifiableMap(transitionRules);
    }
    
    /**
     * Get transition rules as TransitionRule objects
     */
    public List<TransitionRule> getTransitionRules() {
        return transitionRules.entrySet().stream()
            .map(entry -> new TransitionRule(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }
    
    /**
     * Validate that all rules are properly configured
     * This method is called during system startup
     */
    public boolean validateConfiguration() {
        try {
            return validateBasicConfiguration() && validateBusinessLogic();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate basic configuration requirements
     */
    private boolean validateBasicConfiguration() {
        // Check that all OrderStatus values have rules defined
        for (OrderStatus status : OrderStatus.values()) {
            if (!transitionRules.containsKey(status)) {
                return false;
            }
        }
        
        // Check that all referenced states in rules are valid
        for (List<OrderStatus> nextStates : transitionRules.values()) {
            for (OrderStatus nextState : nextStates) {
                if (!transitionRules.containsKey(nextState)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Validate business logic requirements
     */
    private boolean validateBusinessLogic() {
        // Validate that final states have no outgoing transitions
        for (OrderStatus status : OrderStatus.values()) {
            if (status.isFinalState()) {
                List<OrderStatus> transitions = transitionRules.get(status);
                if (transitions != null && !transitions.isEmpty()) {
                    return false;
                }
            }
        }
        
        // Validate that non-final states have at least one transition
        for (OrderStatus status : OrderStatus.values()) {
            if (!status.isFinalState()) {
                List<OrderStatus> transitions = transitionRules.get(status);
                if (transitions == null || transitions.isEmpty()) {
                    return false;
                }
            }
        }
        
        return true;
    }
}