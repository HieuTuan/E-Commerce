package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StateTransitionValidatorImpl
 */
@ExtendWith(MockitoExtension.class)
class StateTransitionValidatorImplTest {
    
    @Mock
    private StateTransitionRules stateTransitionRules;
    
    private StateTransitionValidatorImpl validator;
    
    @BeforeEach
    void setUp() {
        validator = new StateTransitionValidatorImpl(stateTransitionRules);
    }
    
    @Test
    void testValidTransition() {
        // Arrange
        when(stateTransitionRules.isValidTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED))
            .thenReturn(true);
        
        // Act
        ValidationResult result = validator.validateTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        
        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertNull(result.getValidTransitions());
    }
    
    @Test
    void testInvalidTransition() {
        // Arrange
        List<OrderStatus> validTransitions = Arrays.asList(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        when(stateTransitionRules.isValidTransition(OrderStatus.PENDING, OrderStatus.DELIVERED))
            .thenReturn(false);
        when(stateTransitionRules.getValidNextStates(OrderStatus.PENDING))
            .thenReturn(validTransitions);
        
        // Act
        ValidationResult result = validator.validateTransition(OrderStatus.PENDING, OrderStatus.DELIVERED);
        
        // Assert
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Invalid transition from PENDING to DELIVERED"));
        assertEquals(validTransitions, result.getValidTransitions());
    }
    
    @Test
    void testNullCurrentState() {
        // Act
        ValidationResult result = validator.validateTransition(null, OrderStatus.CONFIRMED);
        
        // Assert
        assertFalse(result.isValid());
        assertEquals("Current state cannot be null", result.getErrorMessage());
    }
    
    @Test
    void testNullNewState() {
        // Act
        ValidationResult result = validator.validateTransition(OrderStatus.PENDING, null);
        
        // Assert
        assertFalse(result.isValid());
        assertEquals("New state cannot be null", result.getErrorMessage());
    }
    
    @Test
    void testGetValidNextStates() {
        // Arrange
        List<OrderStatus> expectedStates = Arrays.asList(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        when(stateTransitionRules.getValidNextStates(OrderStatus.PENDING))
            .thenReturn(expectedStates);
        
        // Act
        List<OrderStatus> result = validator.getValidNextStates(OrderStatus.PENDING);
        
        // Assert
        assertEquals(expectedStates, result);
    }
    
    @Test
    void testGetValidNextStatesWithNullState() {
        // Act
        List<OrderStatus> result = validator.getValidNextStates(null);
        
        // Assert
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testIsValidTransition() {
        // Arrange
        when(stateTransitionRules.isValidTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED))
            .thenReturn(true);
        when(stateTransitionRules.isValidTransition(OrderStatus.PENDING, OrderStatus.DELIVERED))
            .thenReturn(false);
        
        // Act & Assert
        assertTrue(validator.isValidTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED));
        assertFalse(validator.isValidTransition(OrderStatus.PENDING, OrderStatus.DELIVERED));
    }
    
    @Test
    void testIsValidTransitionWithNullStates() {
        // Act & Assert
        assertFalse(validator.isValidTransition(null, OrderStatus.CONFIRMED));
        assertFalse(validator.isValidTransition(OrderStatus.PENDING, null));
        assertFalse(validator.isValidTransition(null, null));
    }
    
    @Test
    void testFinalStateTransition() {
        // Arrange
        when(stateTransitionRules.isValidTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED))
            .thenReturn(false);
        when(stateTransitionRules.getValidNextStates(OrderStatus.CANCELLED))
            .thenReturn(Collections.emptyList());
        
        // Act
        ValidationResult result = validator.validateTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("none (final state)"));
    }
}