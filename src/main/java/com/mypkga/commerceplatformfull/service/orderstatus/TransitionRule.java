package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a state transition rule.
 * Defines which states can be transitioned to from a given state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitionRule {
    
    /**
     * The source state
     */
    private OrderStatus fromState;
    
    /**
     * List of allowed destination states
     */
    private List<OrderStatus> allowedNextStates;
    
    /**
     * Check if a transition to the given state is allowed
     */
    public boolean isTransitionAllowed(OrderStatus toState) {
        return allowedNextStates != null && allowedNextStates.contains(toState);
    }
}