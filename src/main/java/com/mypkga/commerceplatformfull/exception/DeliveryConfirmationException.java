package com.mypkga.commerceplatformfull.exception;

/**
 * Exception thrown when there are issues with delivery confirmation operations.
 */
public class DeliveryConfirmationException extends RuntimeException {
    
    public DeliveryConfirmationException(String message) {
        super(message);
    }
    
    public DeliveryConfirmationException(String message, Throwable cause) {
        super(message, cause);
    }
}