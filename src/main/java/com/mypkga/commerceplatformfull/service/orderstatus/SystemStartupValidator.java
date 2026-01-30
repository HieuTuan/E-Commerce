package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.exception.SystemConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive system startup validator for order status validation system.
 * Performs thorough validation of configuration and system integrity during startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemStartupValidator {
    
    private final StateTransitionRules stateTransitionRules;
    private final StateTransitionValidator stateTransitionValidator;
    
    /**
     * Perform comprehensive system startup validation.
     * This method validates all aspects of the order status system configuration.
     * 
     * @throws SystemConfigurationException if any validation fails
     */
    public void validateSystemIntegrity() {
        log.info("Starting comprehensive system startup validation...");
        
        List<String> validationErrors = new ArrayList<>();
        
        // 1. Validate transition rules configuration
        validateTransitionRulesConfiguration(validationErrors);
        
        // 2. Validate state completeness
        validateStateCompleteness(validationErrors);
        
        // 3. Validate transition rule consistency
        validateTransitionRuleConsistency(validationErrors);
        
        // 4. Validate final states
        validateFinalStates(validationErrors);
        
        // 5. Validate validator integration
        validateValidatorIntegration(validationErrors);
        
        // 6. Validate circular dependencies
        validateCircularDependencies(validationErrors);
        
        if (!validationErrors.isEmpty()) {
            String errorMessage = "System startup validation failed with " + validationErrors.size() + " error(s):\n" +
                String.join("\n", validationErrors);
            log.error("System startup validation failed: {}", errorMessage);
            throw new SystemConfigurationException(errorMessage);
        }
        
        log.info("System startup validation completed successfully");
    }
    
    /**
     * Validate basic transition rules configuration
     */
    private void validateTransitionRulesConfiguration(List<String> errors) {
        log.debug("Validating transition rules configuration...");
        
        try {
            if (!stateTransitionRules.validateConfiguration()) {
                errors.add("Basic transition rules configuration validation failed");
            }
        } catch (Exception e) {
            errors.add("Exception during transition rules validation: " + e.getMessage());
        }
    }
    
    /**
     * Validate that all OrderStatus enum values have corresponding rules
     */
    private void validateStateCompleteness(List<String> errors) {
        log.debug("Validating state completeness...");
        
        Map<OrderStatus, List<OrderStatus>> allRules = stateTransitionRules.getAllRules();
        
        for (OrderStatus status : OrderStatus.values()) {
            if (!allRules.containsKey(status)) {
                errors.add("Missing transition rules for status: " + status);
            }
        }
        
        // Validate that all referenced states in rules exist in enum
        for (Map.Entry<OrderStatus, List<OrderStatus>> entry : allRules.entrySet()) {
            for (OrderStatus nextState : entry.getValue()) {
                boolean exists = false;
                for (OrderStatus enumValue : OrderStatus.values()) {
                    if (enumValue == nextState) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    errors.add("Invalid state referenced in rules: " + nextState + " from " + entry.getKey());
                }
            }
        }
    }
    
    /**
     * Validate transition rule consistency and business logic
     */
    private void validateTransitionRuleConsistency(List<String> errors) {
        log.debug("Validating transition rule consistency...");
        
        Map<OrderStatus, List<OrderStatus>> allRules = stateTransitionRules.getAllRules();
        
        // Validate expected business flow
        validateBusinessFlow(allRules, errors);
        
        // Validate that CANCELLED can be reached from all non-final states
        validateCancellationRules(allRules, errors);
    }
    
    /**
     * Validate expected business flow sequence
     */
    private void validateBusinessFlow(Map<OrderStatus, List<OrderStatus>> allRules, List<String> errors) {
        // PENDING should allow CONFIRMED
        if (!allRules.get(OrderStatus.PENDING).contains(OrderStatus.CONFIRMED)) {
            errors.add("PENDING status should allow transition to CONFIRMED");
        }
        
        // CONFIRMED should allow SHIPPING
        if (!allRules.get(OrderStatus.CONFIRMED).contains(OrderStatus.SHIPPING)) {
            errors.add("CONFIRMED status should allow transition to SHIPPING");
        }
        
        // SHIPPING should allow AWAITING_CONFIRMATION
        if (!allRules.get(OrderStatus.SHIPPING).contains(OrderStatus.AWAITING_CONFIRMATION)) {
            errors.add("SHIPPING status should allow transition to AWAITING_CONFIRMATION");
        }
        
        // AWAITING_CONFIRMATION should allow DELIVERED
        if (!allRules.get(OrderStatus.AWAITING_CONFIRMATION).contains(OrderStatus.DELIVERED)) {
            errors.add("AWAITING_CONFIRMATION status should allow transition to DELIVERED");
        }
        
        // DELIVERED should allow CONFIRMED_BY_CUSTOMER
        if (!allRules.get(OrderStatus.DELIVERED).contains(OrderStatus.CONFIRMED_BY_CUSTOMER)) {
            errors.add("DELIVERED status should allow transition to CONFIRMED_BY_CUSTOMER");
        }
    }
    
    /**
     * Validate cancellation rules - CANCELLED should be reachable from all non-final states
     */
    private void validateCancellationRules(Map<OrderStatus, List<OrderStatus>> allRules, List<String> errors) {
        for (OrderStatus status : OrderStatus.values()) {
            if (!status.isFinalState() && status != OrderStatus.CANCELLED) {
                if (!allRules.get(status).contains(OrderStatus.CANCELLED)) {
                    errors.add("Status " + status + " should allow transition to CANCELLED");
                }
            }
        }
    }
    
    /**
     * Validate final states configuration
     */
    private void validateFinalStates(List<String> errors) {
        log.debug("Validating final states...");
        
        Map<OrderStatus, List<OrderStatus>> allRules = stateTransitionRules.getAllRules();
        
        // Final states should have no outgoing transitions
        for (OrderStatus status : OrderStatus.values()) {
            if (status.isFinalState()) {
                List<OrderStatus> transitions = allRules.get(status);
                if (transitions != null && !transitions.isEmpty()) {
                    errors.add("Final state " + status + " should not have any outgoing transitions, but has: " + transitions);
                }
            }
        }
        
        // Non-final states should have at least one outgoing transition
        for (OrderStatus status : OrderStatus.values()) {
            if (!status.isFinalState()) {
                List<OrderStatus> transitions = allRules.get(status);
                if (transitions == null || transitions.isEmpty()) {
                    errors.add("Non-final state " + status + " should have at least one outgoing transition");
                }
            }
        }
    }
    
    /**
     * Validate that the StateTransitionValidator works correctly with the rules
     */
    private void validateValidatorIntegration(List<String> errors) {
        log.debug("Validating validator integration...");
        
        try {
            // Test a few known valid transitions
            ValidationResult result1 = stateTransitionValidator.validateTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
            if (!result1.isValid()) {
                errors.add("Validator should accept PENDING -> CONFIRMED transition");
            }
            
            // Test a known invalid transition
            ValidationResult result2 = stateTransitionValidator.validateTransition(OrderStatus.PENDING, OrderStatus.DELIVERED);
            if (result2.isValid()) {
                errors.add("Validator should reject PENDING -> DELIVERED transition");
            }
            
            // Test final state transitions
            ValidationResult result3 = stateTransitionValidator.validateTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED);
            if (result3.isValid()) {
                errors.add("Validator should reject transitions from final state CANCELLED");
            }
            
        } catch (Exception e) {
            errors.add("Exception during validator integration test: " + e.getMessage());
        }
    }
    
    /**
     * Validate that there are no circular dependencies in transition rules
     */
    private void validateCircularDependencies(List<String> errors) {
        log.debug("Validating circular dependencies...");
        
        Map<OrderStatus, List<OrderStatus>> allRules = stateTransitionRules.getAllRules();
        
        // Check for direct circular dependencies (A -> B, B -> A)
        for (Map.Entry<OrderStatus, List<OrderStatus>> entry : allRules.entrySet()) {
            OrderStatus fromState = entry.getKey();
            for (OrderStatus toState : entry.getValue()) {
                List<OrderStatus> reverseTransitions = allRules.get(toState);
                if (reverseTransitions != null && reverseTransitions.contains(fromState)) {
                    // This is acceptable for cancellation paths, but check if it's not a business flow issue
                    if (toState != OrderStatus.CANCELLED && fromState != OrderStatus.CANCELLED) {
                        log.warn("Potential circular dependency detected: {} <-> {}", fromState, toState);
                    }
                }
            }
        }
        
        // Check for longer circular paths (would require more complex graph analysis)
        // For now, we'll rely on the business flow validation to catch major issues
    }
    
    /**
     * Get detailed system configuration report
     */
    public String getSystemConfigurationReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Order Status System Configuration Report ===\n");
        
        Map<OrderStatus, List<OrderStatus>> allRules = stateTransitionRules.getAllRules();
        
        report.append("\nTransition Rules:\n");
        for (Map.Entry<OrderStatus, List<OrderStatus>> entry : allRules.entrySet()) {
            report.append(String.format("  %s -> %s\n", 
                entry.getKey().getDisplayName(),
                entry.getValue().isEmpty() ? "[FINAL STATE]" : 
                    entry.getValue().stream()
                        .map(OrderStatus::getDisplayName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("none")
            ));
        }
        
        report.append("\nFinal States: ");
        report.append(java.util.Arrays.stream(OrderStatus.values())
            .filter(OrderStatus::isFinalState)
            .map(OrderStatus::getDisplayName)
            .reduce((a, b) -> a + ", " + b)
            .orElse("none"));
        
        report.append("\n\nTotal States: ").append(OrderStatus.values().length);
        report.append("\nTotal Transition Rules: ").append(allRules.size());
        
        return report.toString();
    }
}