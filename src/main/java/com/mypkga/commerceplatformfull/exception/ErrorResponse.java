package com.mypkga.commerceplatformfull.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Enhanced error response structure for comprehensive error handling.
 * 
 * This class provides a standardized format for all API error responses,
 * including detailed error information, validation errors, and contextual data
 * to help clients understand and handle errors appropriately.
 * 
 * Requirements: 2.2, 4.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred (ISO format)
     */
    private String timestamp;
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * HTTP status reason phrase
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * API path where the error occurred
     */
    private String path;
    
    /**
     * Specific error code for programmatic handling
     */
    private String errorCode;
    
    /**
     * Field-specific validation errors
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Additional contextual details about the error
     */
    private Map<String, Object> details;
    
    /**
     * List of suggested actions or valid alternatives
     */
    private List<String> suggestions;
    
    /**
     * Reference ID for error tracking and support
     */
    private String errorId;
    
    /**
     * Nested error information for complex error scenarios
     */
    private ErrorResponse cause;
}