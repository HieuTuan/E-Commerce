package com.mypkga.commerceplatformfull.exception;

/**
 * Exception thrown when there are issues with order timeline operations.
 */
public class OrderTimelineException extends RuntimeException {
    
    public OrderTimelineException(String message) {
        super(message);
    }
    
    public OrderTimelineException(String message, Throwable cause) {
        super(message, cause);
    }
}