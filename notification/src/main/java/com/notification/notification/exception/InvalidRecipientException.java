package com.notification.notification.exception;

/**
 * Exception thrown when recipient data is invalid (e.g., malformed email, invalid phone number).
 * This represents a non-retryable failure that requires correction of input data.
 */
public class InvalidRecipientException extends NotificationException {
    
    public InvalidRecipientException(String message) {
        super(message);
    }
    
    public InvalidRecipientException(String message, Throwable cause) {
        super(message, cause);
    }
}
