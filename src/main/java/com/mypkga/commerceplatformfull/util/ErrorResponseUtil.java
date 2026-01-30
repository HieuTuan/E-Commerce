package com.mypkga.commerceplatformfull.util;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.exception.ErrorResponse;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility class for creating standardized error responses.
 * 
 * This utility provides methods to create consistent error responses
 * with appropriate HTTP status codes, error messages, and contextual information
 * for order status validation errors.
 * 
 * Requirements: 2.2, 4.4
 */
public class ErrorResponseUtil {
    
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    
    /**
     * Create a basic error response
     */
    public static ErrorResponse createErrorResponse(HttpStatus status, String errorCode, 
                                                  String message, String path) {
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)))
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(path)
            .errorCode(errorCode)
            .errorId(generateErrorId())
            .build();
    }
    
    /**
     * Create error response for invalid status transitions
     */
    public static ErrorResponse createInvalidTransitionError(Long orderId, OrderStatus currentStatus, 
                                                           OrderStatus attemptedStatus, 
                                                           List<OrderStatus> validTransitions, 
                                                           String path) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", orderId);
        details.put("currentStatus", currentStatus != null ? currentStatus.name() : null);
        details.put("currentStatusDisplayName", currentStatus != null ? currentStatus.getDisplayName() : null);
        details.put("attemptedStatus", attemptedStatus != null ? attemptedStatus.name() : null);
        details.put("attemptedStatusDisplayName", attemptedStatus != null ? attemptedStatus.getDisplayName() : null);
        
        if (validTransitions != null && !validTransitions.isEmpty()) {
            List<Map<String, String>> validOptions = new ArrayList<>();
            for (OrderStatus status : validTransitions) {
                Map<String, String> option = new HashMap<>();
                option.put("status", status.name());
                option.put("displayName", status.getDisplayName());
                validOptions.add(option);
            }
            details.put("validTransitions", validOptions);
        }
        
        List<String> suggestions = createTransitionSuggestions(currentStatus, validTransitions);
        
        String message = String.format("Cannot transition order %d from %s to %s", 
            orderId, 
            currentStatus != null ? currentStatus.getDisplayName() : "unknown",
            attemptedStatus != null ? attemptedStatus.getDisplayName() : "unknown");
        
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)))
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .message(message)
            .path(path)
            .errorCode("INVALID_TRANSITION")
            .details(details)
            .suggestions(suggestions)
            .errorId(generateErrorId())
            .build();
    }
    
    /**
     * Create error response for final state modification attempts
     */
    public static ErrorResponse createFinalStateModificationError(Long orderId, OrderStatus currentStatus, 
                                                                OrderStatus attemptedStatus, String path) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", orderId);
        details.put("currentStatus", currentStatus.name());
        details.put("currentStatusDisplayName", currentStatus.getDisplayName());
        details.put("attemptedStatus", attemptedStatus.name());
        details.put("attemptedStatusDisplayName", attemptedStatus.getDisplayName());
        details.put("isFinalState", true);
        
        List<String> suggestions = Arrays.asList(
            "Final states cannot be modified",
            "Contact administrator if status correction is needed",
            "Create a new order if needed"
        );
        
        String message = String.format("Cannot modify order %d status from final state %s. Final states are immutable.", 
            orderId, currentStatus.getDisplayName());
        
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)))
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .message(message)
            .path(path)
            .errorCode("FINAL_STATE_MODIFICATION")
            .details(details)
            .suggestions(suggestions)
            .errorId(generateErrorId())
            .build();
    }
    
    /**
     * Create error response for order not found
     */
    public static ErrorResponse createOrderNotFoundError(Long orderId, String path) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", orderId);
        
        List<String> suggestions = Arrays.asList(
            "Verify the order ID is correct",
            "Check if the order exists in the system",
            "Contact support if the order should exist"
        );
        
        String message = String.format("Order with ID %d not found", orderId);
        
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)))
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(message)
            .path(path)
            .errorCode("ORDER_NOT_FOUND")
            .details(details)
            .suggestions(suggestions)
            .errorId(generateErrorId())
            .build();
    }
    
    /**
     * Create error response for validation errors
     */
    public static ErrorResponse createValidationError(String message, String path, 
                                                    Map<String, String> fieldErrors) {
        List<String> suggestions = Arrays.asList(
            "Check the request format and required fields",
            "Ensure all field values are valid",
            "Refer to API documentation for correct format"
        );
        
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)))
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .path(path)
            .errorCode("VALIDATION_ERROR")
            .fieldErrors(fieldErrors)
            .suggestions(suggestions)
            .errorId(generateErrorId())
            .build();
    }
    
    /**
     * Create error response for system errors
     */
    public static ErrorResponse createSystemError(String message, String path) {
        List<String> suggestions = Arrays.asList(
            "Try the request again later",
            "Contact support if the problem persists",
            "Check system status page for known issues"
        );
        
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)))
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message(message != null ? message : "An unexpected system error occurred")
            .path(path)
            .errorCode("SYSTEM_ERROR")
            .suggestions(suggestions)
            .errorId(generateErrorId())
            .build();
    }
    
    /**
     * Create suggestions for valid transitions
     */
    private static List<String> createTransitionSuggestions(OrderStatus currentStatus, 
                                                          List<OrderStatus> validTransitions) {
        List<String> suggestions = new ArrayList<>();
        
        if (currentStatus != null) {
            suggestions.add(String.format("Current order status is %s", currentStatus.getDisplayName()));
        }
        
        if (validTransitions != null && !validTransitions.isEmpty()) {
            StringBuilder validOptions = new StringBuilder("Valid next statuses: ");
            for (int i = 0; i < validTransitions.size(); i++) {
                if (i > 0) validOptions.append(", ");
                validOptions.append(validTransitions.get(i).getDisplayName());
            }
            suggestions.add(validOptions.toString());
        } else {
            suggestions.add("No status transitions are allowed from the current state");
        }
        
        suggestions.add("Use GET /api/orders/{id}/status-options to see available options");
        
        return suggestions;
    }
    
    /**
     * Generate a unique error ID for tracking
     */
    private static String generateErrorId() {
        return "ERR-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(new Random().nextInt(0xFFFF)).toUpperCase();
    }
}