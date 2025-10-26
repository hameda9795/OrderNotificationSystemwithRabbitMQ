package com.notification.notification.exception;

/**
 * Exception thrown when an external service (email/SMS provider) is unavailable.
 * This typically represents transient failures that may be resolved by retrying.
 */
public class ServiceUnavailableException extends NotificationException {

    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException() {
        super("Service temporarily unavailable");
    }

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(Throwable cause) {
        super("Service temporarily unavailable", cause);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
