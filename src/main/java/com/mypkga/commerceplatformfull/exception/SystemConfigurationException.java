package com.mypkga.commerceplatformfull.exception;

/**
 * Exception thrown when system configuration validation fails during startup.
 * This indicates critical configuration errors that prevent the system from operating correctly.
 */
public class SystemConfigurationException extends RuntimeException {
    
    public SystemConfigurationException(String message) {
        super(message);
    }
    
    public SystemConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}