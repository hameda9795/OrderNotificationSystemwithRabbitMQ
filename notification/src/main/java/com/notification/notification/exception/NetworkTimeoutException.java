package com.notification.notification.exception;

/**
 * Exception thrown when a network timeout occurs while communicating with external services.
 * This represents a transient failure that may succeed on retry.
 */
public class NetworkTimeoutException extends NotificationException {
    
    public NetworkTimeoutException(String message) {
        super(message);
    }
    
    public NetworkTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
