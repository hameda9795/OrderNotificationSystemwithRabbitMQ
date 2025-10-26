package com.notification.notification.exception;

/**
 * Exception thrown when an external service (email/SMS provider) is unavailable.
 * This typically represents transient failures that may be resolved by retrying.
 */
public class ServiceUnavailableException extends NotificationException {
    
    public ServiceUnavailableException(String message) {
        super(message);
    }
    
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
