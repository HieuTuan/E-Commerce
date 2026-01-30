package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.StatusUpdateRequest;
import com.mypkga.commerceplatformfull.dto.StatusUpdateResponse;
import com.mypkga.commerceplatformfull.dto.StatusOptionsResponse;
import com.mypkga.commerceplatformfull.dto.CurrentStatusResponse;
import com.mypkga.commerceplatformfull.dto.CustomerConfirmationRequest;
import com.mypkga.commerceplatformfull.dto.CustomerConfirmationResponse;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.exception.ErrorResponse;
import com.mypkga.commerceplatformfull.service.orderstatus.OrderStatusManager;
import com.mypkga.commerceplatformfull.service.orderstatus.StatusOption;
import com.mypkga.commerceplatformfull.service.orderstatus.UIIntegrationService;
import com.mypkga.commerceplatformfull.service.orderstatus.UpdateResult;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.util.ErrorResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Order Status Management
 * 
 * Provides endpoints for:
 * - PUT /orders/{id}/status: Update order status with validation
 * - GET /orders/{id}/status-options: Get available status options for UI
 * 
 * Integrates with OrderStatusManager and UIIntegrationService to provide
 * validated status updates and UI-friendly status options.
 * 
 * Requirements: 4.3, 4.4
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderStatusApiController {

    private final OrderStatusManager orderStatusManager;
    private final UIIntegrationService uiIntegrationService;
    private final UserRepository userRepository;

    /**
     * Update order status with validation
     * <p>
     * PUT /api/orders/{id}/status
     *
     * @param orderId        The ID of the order to update
     * @param request        The status update request containing new status and optional notes
     * @param authentication Current user authentication
     * @return ResponseEntity with update result or error details
     * <p>
     * Requirements: 4.3 - Status update with validation
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication) {

        try {
            log.info("Received status update request for order {} to status {} by user: {}",
                    orderId, request.getStatus(), authentication.getName());

            // Validate input parameters
            if (orderId == null || orderId <= 0) {
                log.warn("Invalid order ID provided: {}", orderId);
                ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ORDER_ID",
                        "Order ID must be a positive number",
                        "/api/orders/" + orderId + "/status"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            if (request.getStatus() == null) {
                log.warn("Status update request missing status for order: {}", orderId);
                Map<String, String> fieldErrors = new HashMap<>();
                fieldErrors.put("status", "Status is required and cannot be null");
                ErrorResponse errorResponse = ErrorResponseUtil.createValidationError(
                        "Status is required",
                        "/api/orders/" + orderId + "/status",
                        fieldErrors
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Get current user ID from authentication
            String userId = authentication.getName();

            // Perform status update with validation
            UpdateResult result = orderStatusManager.updateOrderStatus(orderId, request.getStatus(), userId);

            if (result.isSuccess()) {
                log.info("Successfully updated order {} status from {} to {} by user: {}",
                        orderId, result.getPreviousStatus(), result.getNewStatus(), userId);

                // Return success response with update details
                StatusUpdateResponse successResponse = StatusUpdateResponse.success(
                        orderId,
                        result.getPreviousStatus(),
                        result.getNewStatus(),
                        userId,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        request.getNotes(),
                        request.getReasonCode(),
                        request.isForceUpdate()
                );

                return ResponseEntity.ok(successResponse);
            } else {
                log.warn("Status update failed for order {} to status {} by user: {}. Error: {}",
                        orderId, request.getStatus(), userId, result.getErrorMessage());

                // Determine appropriate HTTP status and create detailed error response
                String errorMessage = result.getErrorMessage();

                if (errorMessage.toLowerCase().contains("not found")) {
                    ErrorResponse errorResponse = ErrorResponseUtil.createOrderNotFoundError(
                            orderId, "/api/orders/" + orderId + "/status");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                } else if (errorMessage.toLowerCase().contains("final state") ||
                        errorMessage.toLowerCase().contains("cannot be changed")) {
                    ErrorResponse errorResponse = ErrorResponseUtil.createFinalStateModificationError(
                            orderId, result.getPreviousStatus(), request.getStatus(),
                            "/api/orders/" + orderId + "/status");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                } else {
                    // Generic validation error
                    ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                            HttpStatus.BAD_REQUEST,
                            "VALIDATION_ERROR",
                            errorMessage,
                            "/api/orders/" + orderId + "/status"
                    );
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for status update on order {}: {}", orderId, e.getMessage());
            ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ARGUMENT",
                    e.getMessage(),
                    "/api/orders/" + orderId + "/status"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error updating status for order {} to {}: {}",
                    orderId, request.getStatus(), e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponseUtil.createSystemError(
                    "An unexpected error occurred while updating order status",
                    "/api/orders/" + orderId + "/status"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get available status options for an order
     * <p>
     * GET /api/orders/{id}/status-options
     *
     * @param orderId The ID of the order
     * @return ResponseEntity with list of available status options or error details
     * <p>
     * Requirements: 4.4 - UI integration for status options
     */
    @GetMapping("/{id}/status-options")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> getStatusOptions(@PathVariable("id") Long orderId) {

        try {
            log.debug("Received request for status options for order: {}", orderId);

            // Validate input parameters
            if (orderId == null || orderId <= 0) {
                log.warn("Invalid order ID provided for status options: {}", orderId);
                ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ORDER_ID",
                        "Order ID must be a positive number",
                        "/api/orders/" + orderId + "/status-options"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Get available status options from UI integration service
            List<StatusOption> statusOptions = uiIntegrationService.getAvailableStatusOptions(orderId);

            // Check if order can change status
            boolean canChangeStatus = uiIntegrationService.canChangeStatus(orderId);

            // Get customer delivery confirmation option if applicable
            StatusOption customerConfirmationOption = uiIntegrationService.getCustomerDeliveryConfirmationOption(orderId);

            // Check if customer delivery confirmation is required
            boolean requiresCustomerConfirmation = uiIntegrationService.requiresCustomerDeliveryConfirmation(orderId);

            // Get current status for metadata
            OrderStatus currentStatus = orderStatusManager.getCurrentStatus(orderId);

            log.debug("Found {} status options for order {}, canChangeStatus: {}, requiresCustomerConfirmation: {}",
                    statusOptions.size(), orderId, canChangeStatus, requiresCustomerConfirmation);

            // Build metadata
            StatusOptionsResponse.OrderStatusMetadata metadata = StatusOptionsResponse.OrderStatusMetadata.builder()
                    .currentStatus(currentStatus.name())
                    .currentStatusDisplayName(currentStatus.getDisplayName())
                    .isFinalState(currentStatus.isFinalState())
                    .availableTransitionsCount(statusOptions.size())
                    .customerActionable(requiresCustomerConfirmation)
                    .build();

            // Build comprehensive response
            StatusOptionsResponse response = StatusOptionsResponse.builder()
                    .orderId(orderId)
                    .canChangeStatus(canChangeStatus)
                    .statusOptions(statusOptions)
                    .requiresCustomerConfirmation(requiresCustomerConfirmation)
                    .customerConfirmationOption(customerConfirmationOption)
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .metadata(metadata)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for status options request on order {}: {}", orderId, e.getMessage());
            if (e.getMessage().toLowerCase().contains("not found")) {
                ErrorResponse errorResponse = ErrorResponseUtil.createOrderNotFoundError(
                        orderId, "/api/orders/" + orderId + "/status-options");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ARGUMENT",
                        e.getMessage(),
                        "/api/orders/" + orderId + "/status-options"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Unexpected error getting status options for order {}: {}", orderId, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponseUtil.createSystemError(
                    "An unexpected error occurred while retrieving status options",
                    "/api/orders/" + orderId + "/status-options"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get current order status
     * <p>
     * GET /api/orders/{id}/status
     *
     * @param orderId The ID of the order
     * @return ResponseEntity with current status information or error details
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> getCurrentStatus(@PathVariable("id") Long orderId) {

        try {
            log.debug("Received request for current status of order: {}", orderId);

            // Validate input parameters
            if (orderId == null || orderId <= 0) {
                log.warn("Invalid order ID provided for status query: {}", orderId);
                ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ORDER_ID",
                        "Order ID must be a positive number",
                        "/api/orders/" + orderId + "/status"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Get current status from order status manager
            OrderStatus currentStatus = orderStatusManager.getCurrentStatus(orderId);

            log.debug("Retrieved current status {} for order: {}", currentStatus, orderId);

            // Return current status information using standardized response
            CurrentStatusResponse response = CurrentStatusResponse.from(
                    orderId,
                    currentStatus,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for status query on order {}: {}", orderId, e.getMessage());
            ErrorResponse errorResponse = ErrorResponseUtil.createOrderNotFoundError(
                    orderId, "/api/orders/" + orderId + "/status");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error getting current status for order {}: {}", orderId, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponseUtil.createSystemError(
                    "An unexpected error occurred while retrieving order status",
                    "/api/orders/" + orderId + "/status"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Customer confirmation endpoint for delivery
     * 
     * PUT /api/orders/{id}/confirm-delivery
     * 
     * @param orderId The ID of the order to confirm
     * @param request The customer confirmation request with optional notes
     * @param authentication Current user authentication
     * @return ResponseEntity with confirmation result or error details
     * 
     * Requirements: 1.5, 4.1 - Customer delivery confirmation
     */
    @PutMapping("/{id}/confirm-delivery")
    @PreAuthorize("hasRole('USER') or hasRole('CUSTOMER')")
    public ResponseEntity<?> confirmDelivery(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody CustomerConfirmationRequest request,
            Authentication authentication) {
        
        try {
            log.info("Received customer delivery confirmation request for order {} by user: {}", 
                orderId, authentication.getName());
            
            // Validate input parameters
            if (orderId == null || orderId <= 0) {
                log.warn("Invalid order ID provided for customer confirmation: {}", orderId);
                ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ORDER_ID",
                    "Order ID must be a positive number",
                    "/api/orders/" + orderId + "/confirm-delivery"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Get customer ID from authentication
            String username = authentication.getName();
            
            // Look up the user by username to get the customer ID
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                log.warn("User not found for username: {}", username);
                ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    "USER_NOT_FOUND",
                    "User not found for the provided authentication",
                    "/api/orders/" + orderId + "/confirm-delivery"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            Long customerId = user.getId();
            
            // Perform customer delivery confirmation
            UpdateResult result = orderStatusManager.confirmDeliveryByCustomer(orderId, customerId, request.getNotes());
            
            if (result.isSuccess()) {
                log.info("Successfully confirmed delivery for order {} by customer: {}", orderId, customerId);
                
                // Return success response with confirmation details
                CustomerConfirmationResponse successResponse = CustomerConfirmationResponse.success(
                    orderId,
                    result.getPreviousStatus(),
                    result.getNewStatus(),
                    customerId.toString(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    request.getNotes(),
                    request.getDeliveryRating()
                );
                
                return ResponseEntity.ok(successResponse);
            } else {
                log.warn("Customer delivery confirmation failed for order {} by customer: {}. Error: {}", 
                    orderId, customerId, result.getErrorMessage());
                
                // Determine appropriate HTTP status and create detailed error response
                String errorMessage = result.getErrorMessage();
                
                if (errorMessage.toLowerCase().contains("not found")) {
                    ErrorResponse errorResponse = ErrorResponseUtil.createOrderNotFoundError(
                        orderId, "/api/orders/" + orderId + "/confirm-delivery");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                } else if (errorMessage.toLowerCase().contains("not authorized") || 
                          errorMessage.toLowerCase().contains("unauthorized")) {
                    ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                        HttpStatus.FORBIDDEN,
                        "UNAUTHORIZED_ACCESS",
                        errorMessage,
                        "/api/orders/" + orderId + "/confirm-delivery"
                    );
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                } else if (errorMessage.toLowerCase().contains("invalid status") || 
                          errorMessage.toLowerCase().contains("must be in delivered")) {
                    ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                        HttpStatus.CONFLICT,
                        "INVALID_ORDER_STATUS",
                        errorMessage,
                        "/api/orders/" + orderId + "/confirm-delivery"
                    );
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                } else {
                    // Generic validation error
                    ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "CONFIRMATION_ERROR",
                        errorMessage,
                        "/api/orders/" + orderId + "/confirm-delivery"
                    );
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for customer delivery confirmation on order {}: {}", orderId, e.getMessage());
            ErrorResponse errorResponse = ErrorResponseUtil.createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT",
                e.getMessage(),
                "/api/orders/" + orderId + "/confirm-delivery"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error confirming delivery for order {} by user {}: {}", 
                orderId, authentication.getName(), e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponseUtil.createSystemError(
                "An unexpected error occurred while confirming delivery",
                "/api/orders/" + orderId + "/confirm-delivery"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}