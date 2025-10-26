package com.notification.notification.exception;

/**
 * Exception thrown when order creation fails due to business logic errors.
 */
public class OrderCreationException extends RuntimeException {
    
    public OrderCreationException(String message) {
        super(message);
    }
    
    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
