package com.mypkga.commerceplatformfull.exception;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        List<String> suggestions = List.of(
                "Check the request format and required fields",
                "Ensure all field values are valid",
                "Refer to API documentation for correct format");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Input validation failed")
                .path(request.getRequestURI())
                .errorCode("VALIDATION_ERROR")
                .fieldErrors(fieldErrors)
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.warn("Validation error on path: {} - Errors: {}", request.getRequestURI(), fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindExceptions(
            BindException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        List<String> suggestions = List.of(
                "Check the form data format",
                "Ensure all required fields are provided",
                "Verify field value constraints");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Binding Failed")
                .message("Form binding validation failed")
                .path(request.getRequestURI())
                .errorCode("BINDING_ERROR")
                .fieldErrors(fieldErrors)
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.warn("Binding error on path: {} - Errors: {}", request.getRequestURI(), fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You don't have permission to access this resource")
                .path(request.getRequestURI())
                .errorCode("ACCESS_DENIED")
                .build();

        log.warn("Access denied for path: {} - User: {}",
                request.getRequestURI(), getCurrentUsername(request));
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("The requested resource was not found")
                .path(request.getRequestURI())
                .errorCode("NOT_FOUND")
                .build();

        log.warn("Resource not found: {}", request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("RESOURCE_NOT_FOUND")
                .build();

        log.error("Runtime exception on path: {} - Message: {}",
                request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderTimelineException.class)
    public ResponseEntity<ErrorResponse> handleOrderTimelineException(
            OrderTimelineException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Order Timeline Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("TIMELINE_ERROR")
                .build();

        log.error("Order timeline error on path: {} - Message: {}",
                request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DeliveryConfirmationException.class)
    public ResponseEntity<ErrorResponse> handleDeliveryConfirmationException(
            DeliveryConfirmationException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Delivery Confirmation Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("DELIVERY_CONFIRMATION_ERROR")
                .build();

        log.error("Delivery confirmation error on path: {} - Message: {}",
                request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DeliveryIssueException.class)
    public ResponseEntity<ErrorResponse> handleDeliveryIssueException(
            DeliveryIssueException ex, HttpServletRequest request) {

        List<String> suggestions = List.of(
                "Ensure the order exists and has DELIVERED status",
                "Check if a delivery issue report already exists for this order",
                "Verify user permissions for the order");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Delivery Issue Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("DELIVERY_ISSUE_ERROR")
                .suggestions(suggestions)
                .build();

        log.warn("Delivery issue error: {} at {}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransitionException(
            InvalidStatusTransitionException ex, HttpServletRequest request) {

        Map<String, String> details = new HashMap<>();
        details.put("currentStatus", ex.getCurrentStatus().name());
        details.put("attemptedStatus", ex.getNewStatus().name());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.CONFLICT.value())
                .error("Invalid Status Transition")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("INVALID_STATUS_TRANSITION")
                .fieldErrors(details)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.warn("Invalid status transition on path: {} - From: {} To: {}",
                request.getRequestURI(), ex.getCurrentStatus(), ex.getNewStatus());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OrderStatusValidationException.class)
    public ResponseEntity<ErrorResponse> handleOrderStatusValidationException(
            OrderStatusValidationException ex, HttpServletRequest request) {

        Map<String, Object> details = new HashMap<>();
        details.put("orderId", ex.getOrderId());

        if (ex.getCurrentStatus() != null) {
            details.put("currentStatus", ex.getCurrentStatus().name());
            details.put("currentStatusDisplayName", ex.getCurrentStatus().getDisplayName());
        }

        if (ex.getAttemptedStatus() != null) {
            details.put("attemptedStatus", ex.getAttemptedStatus().name());
            details.put("attemptedStatusDisplayName", ex.getAttemptedStatus().getDisplayName());
        }

        if (ex.getValidTransitions() != null && !ex.getValidTransitions().isEmpty()) {
            List<Map<String, String>> validOptions = new ArrayList<>();
            for (OrderStatus status : ex.getValidTransitions()) {
                Map<String, String> option = new HashMap<>();
                option.put("status", status.name());
                option.put("displayName", status.getDisplayName());
                validOptions.add(option);
            }
            details.put("validTransitions", validOptions);
        }

        // Create suggestions based on error type
        List<String> suggestions = new ArrayList<>();
        if ("ORDER_NOT_FOUND".equals(ex.getValidationErrorCode())) {
            suggestions.add("Verify the order ID is correct");
            suggestions.add("Check if the order exists in the system");
        } else if ("FINAL_STATE_MODIFICATION".equals(ex.getValidationErrorCode())) {
            suggestions.add("Final states cannot be modified");
            suggestions.add("Contact administrator if status correction is needed");
        } else {
            suggestions.add("Use GET /api/orders/" + ex.getOrderId() + "/status-options to see available options");
            if (ex.getValidTransitions() != null && !ex.getValidTransitions().isEmpty()) {
                StringBuilder validOptions = new StringBuilder("Valid transitions: ");
                for (int i = 0; i < ex.getValidTransitions().size(); i++) {
                    if (i > 0)
                        validOptions.append(", ");
                    validOptions.append(ex.getValidTransitions().get(i).getDisplayName());
                }
                suggestions.add(validOptions.toString());
            }
        }

        // Determine HTTP status based on error code
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        if ("ORDER_NOT_FOUND".equals(ex.getValidationErrorCode())) {
            httpStatus = HttpStatus.NOT_FOUND;
        } else if ("INVALID_TRANSITION".equals(ex.getValidationErrorCode()) ||
                "FINAL_STATE_MODIFICATION".equals(ex.getValidationErrorCode())) {
            httpStatus = HttpStatus.CONFLICT;
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getValidationErrorCode())
                .details(details)
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.warn("Order status validation error on path: {} - Error: {} - Order: {}",
                request.getRequestURI(), ex.getValidationErrorCode(), ex.getOrderId());
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(SystemConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleSystemConfigurationException(
            SystemConfigurationException ex, HttpServletRequest request) {

        List<String> suggestions = List.of(
                "Check system configuration files",
                "Verify order status transition rules are properly defined",
                "Contact system administrator for configuration assistance",
                "Review application logs for detailed error information");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("System Configuration Error")
                .message("System configuration validation failed: " + ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("SYSTEM_CONFIGURATION_ERROR")
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.error("System configuration error on path: {} - Message: {}",
                request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Security Error")
                .message("A security violation occurred")
                .path(request.getRequestURI())
                .errorCode("SECURITY_ERROR")
                .build();

        log.error("Security exception on path: {} - Message: {} - User: {}",
                request.getRequestURI(), ex.getMessage(), getCurrentUsername(request));
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {

        String path = request.getRequestURI();

        // Suppress DevTools and browser-specific resource requests from logging
        if (path.contains(".well-known") || path.contains("devtools") ||
                path.contains("favicon.ico") || path.contains("robots.txt")) {
            // Only log at debug level for these expected missing resources
            log.debug("Browser requested missing resource: {}", path);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Log other missing resources normally
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .message("The requested static resource was not found")
                .path(path)
                .errorCode("STATIC_RESOURCE_NOT_FOUND")
                .build();

        log.warn("Static resource not found: {}", path);
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        List<String> suggestions = List.of(
                "Try the request again later",
                "Contact support if the problem persists",
                "Check system status page for known issues");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .errorCode("INTERNAL_ERROR")
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.error("Unexpected error on path: {} - Exception: {}",
                request.getRequestURI(), ex.getMessage(), ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return request.getRemoteUser() != null ? request.getRemoteUser() : "anonymous";
    }
}