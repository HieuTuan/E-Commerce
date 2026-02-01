package com.mypkga.commerceplatformfull.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Specialized exception handler for return and refund related exceptions.
 * This handler provides specific error responses for return workflow exceptions
 * with appropriate HTTP status codes and detailed error messages.
 * 
 * Requirements: 1.3, 8.6, 9.4
 */
@ControllerAdvice
@Slf4j
public class ReturnExceptionHandler {

    /**
     * Handles ReturnNotEligibleException when return requests are not eligible
     * based on business rules (order status, time window, etc.)
     * 
     * @param ex the ReturnNotEligibleException
     * @param request the HTTP request
     * @return ResponseEntity with BAD_REQUEST status and detailed error information
     */
    @ExceptionHandler(ReturnNotEligibleException.class)
    public ResponseEntity<ErrorResponse> handleReturnNotEligible(
            ReturnNotEligibleException ex, HttpServletRequest request) {
        
        List<String> suggestions = List.of(
            "Check that the order status is 'DELIVERED'",
            "Ensure the return request is within 2 days of delivery",
            "Verify that no existing return request exists for this order",
            "Contact customer support if you believe this is an error"
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Return Not Eligible")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("RETURN_NOT_ELIGIBLE")
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.warn("Return not eligible error on path: {} - Message: {}", 
                request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles InvalidReturnStatusException when invalid return status transitions
     * are attempted, ensuring the return state machine is properly enforced.
     * 
     * @param ex the InvalidReturnStatusException
     * @param request the HTTP request
     * @return ResponseEntity with CONFLICT status and detailed error information
     */
    @ExceptionHandler(InvalidReturnStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReturnStatus(
            InvalidReturnStatusException ex, HttpServletRequest request) {
        
        List<String> suggestions = List.of(
            "Check the current return request status before attempting transitions",
            "Follow the proper return workflow sequence",
            "Valid transitions: REFUND_REQUESTED → RETURN_APPROVED → RETURNING → RETURN_RECEIVED → REFUNDED",
            "Contact administrator if status correction is needed"
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.CONFLICT.value())
                .error("Invalid Return Status")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("INVALID_RETURN_STATUS")
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.warn("Invalid return status error on path: {} - Message: {}", 
                request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles VideoUploadException when video upload operations fail during
     * return request submission or evidence processing.
     * 
     * @param ex the VideoUploadException
     * @param request the HTTP request
     * @return ResponseEntity with BAD_REQUEST status and detailed error information
     */
    @ExceptionHandler(VideoUploadException.class)
    public ResponseEntity<ErrorResponse> handleVideoUpload(
            VideoUploadException ex, HttpServletRequest request) {
        
        List<String> suggestions = List.of(
            "Ensure the video file is in a supported format (MP4, AVI, MOV)",
            "Check that the video file size is within limits (max 100MB)",
            "Verify your internet connection is stable",
            "Try uploading the video again",
            "Contact support if the problem persists"
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Video Upload Failed")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("VIDEO_UPLOAD_ERROR")
                .suggestions(suggestions)
                .errorId("ERR-" + System.currentTimeMillis())
                .build();

        log.error("Video upload error on path: {} - Message: {}", 
                request.getRequestURI(), ex.getMessage(), ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}