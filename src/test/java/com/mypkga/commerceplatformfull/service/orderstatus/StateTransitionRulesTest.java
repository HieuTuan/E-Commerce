package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StateTransitionRules
 */
class StateTransitionRulesTest {
    
    private StateTransitionRules stateTransitionRules;
    
    @BeforeEach
    void setUp() {
        stateTransitionRules = new StateTransitionRules();
    }
    
    @Test
    void testPendingTransitions() {
        List<OrderStatus> validStates = stateTransitionRules.getValidNextStates(OrderStatus.PENDING);
        
        assertEquals(2, validStates.size());
        assertTrue(validStates.contains(OrderStatus.CONFIRMED));
        assertTrue(validStates.contains(OrderStatus.CANCELLED));
    }
    
    @Test
    void testConfirmedTransitions() {
        List<OrderStatus> validStates = stateTransitionRules.getValidNextStates(OrderStatus.CONFIRMED);
        
        assertEquals(2, validStates.size());
        assertTrue(validStates.contains(OrderStatus.SHIPPING));
        assertTrue(validStates.contains(OrderStatus.CANCELLED));
    }
    
    @Test
    void testShippingTransitions() {
        List<OrderStatus> validStates = stateTransitionRules.getValidNextStates(OrderStatus.SHIPPING);
        
        assertEquals(2, validStates.size());
        assertTrue(validStates.contains(OrderStatus.AWAITING_CONFIRMATION));
        assertTrue(validStates.contains(OrderStatus.CANCELLED));
    }
    
    @Test
    void testAwaitingConfirmationTransitions() {
        List<OrderStatus> validStates = stateTransitionRules.getValidNextStates(OrderStatus.AWAITING_CONFIRMATION);
        
        assertEquals(2, validStates.size());
        assertTrue(validStates.contains(OrderStatus.DELIVERED));
        assertTrue(validStates.contains(OrderStatus.CANCELLED));
    }
    
    @Test
    void testDeliveredTransitions() {
        List<OrderStatus> validStates = stateTransitionRules.getValidNextStates(OrderStatus.DELIVERED);
        
        assertEquals(1, validStates.size());
        assertTrue(validStates.contains(OrderStatus.CONFIRMED_BY_CUSTOMER));
    }
    
    @Test
    void testFinalStatesHaveNoTransitions() {
        // CONFIRMED_BY_CUSTOMER should have no valid transitions
        List<OrderStatus> confirmedByCustomerStates = stateTransitionRules.getValidNextStates(OrderStatus.CONFIRMED_BY_CUSTOMER);
        assertTrue(confirmedByCustomerStates.isEmpty());
        
        // CANCELLED should have no valid transitions
        List<OrderStatus> cancelledStates = stateTransitionRules.getValidNextStates(OrderStatus.CANCELLED);
        assertTrue(cancelledStates.isEmpty());
    }
    
    @Test
    void testValidTransitionChecks() {
        // Valid transitions
        assertTrue(stateTransitionRules.isValidTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED));
        assertTrue(stateTransitionRules.isValidTransition(OrderStatus.CONFIRMED, OrderStatus.SHIPPING));
        assertTrue(stateTransitionRules.isValidTransition(OrderStatus.SHIPPING, OrderStatus.AWAITING_CONFIRMATION));
        assertTrue(stateTransitionRules.isValidTransition(OrderStatus.AWAITING_CONFIRMATION, OrderStatus.DELIVERED));
        assertTrue(stateTransitionRules.isValidTransition(OrderStatus.DELIVERED, OrderStatus.CONFIRMED_BY_CUSTOMER));
        
        // Invalid transitions
        assertFalse(stateTransitionRules.isValidTransition(OrderStatus.PENDING, OrderStatus.DELIVERED));
        assertFalse(stateTransitionRules.isValidTransition(OrderStatus.CONFIRMED_BY_CUSTOMER, OrderStatus.PENDING));
        assertFalse(stateTransitionRules.isValidTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));
    }
    
    @Test
    void testConfigurationValidation() {
        assertTrue(stateTransitionRules.validateConfiguration());
    }
    
    @Test
    void testAllOrderStatusesHaveRules() {
        for (OrderStatus status : OrderStatus.values()) {
            List<OrderStatus> nextStates = stateTransitionRules.getValidNextStates(status);
            assertNotNull(nextStates, "Status " + status + " should have transition rules defined");
        }
    }
}