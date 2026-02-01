package com.mypkga.commerceplatformfull.exception;

public class DeliveryIssueException extends RuntimeException {
    
    public DeliveryIssueException(String message) {
        super(message);
    }
    
    public DeliveryIssueException(String message, Throwable cause) {
        super(message, cause);
    }
}