package com.mypkga.commerceplatformfull.exception;

/**
 * Exception thrown when a return request is not eligible based on business rules.
 * This includes cases where the order status is not DELIVERED, the request is outside
 * the 2-day eligibility window, or the order already has a return request.
 */
public class ReturnNotEligibleException extends RuntimeException {
    
    public ReturnNotEligibleException(String message) {
        super(message);
    }
    
    public ReturnNotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}