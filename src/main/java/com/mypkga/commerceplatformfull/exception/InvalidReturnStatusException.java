package com.mypkga.commerceplatformfull.exception;

/**
 * Exception thrown when an invalid return status transition is attempted.
 * This ensures the return request state machine is properly enforced.
 */
public class InvalidReturnStatusException extends RuntimeException {
    
    public InvalidReturnStatusException(String message) {
        super(message);
    }
    
    public InvalidReturnStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}