package com.mypkga.commerceplatformfull.exception;

/**
 * Exception thrown when video upload operations fail
 */
public class VideoUploadException extends RuntimeException {
    
    public VideoUploadException(String message) {
        super(message);
    }
    
    public VideoUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}