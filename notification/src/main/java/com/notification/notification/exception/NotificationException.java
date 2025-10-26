package com.notification.notification.exception;

/**
 * Base exception for notification-related failures.
 * Provides error code support for better error categorization.
 */
public class NotificationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public NotificationException(String message) {
        super(message);
        this.errorCode = "NOTIFICATION_ERROR";
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NOTIFICATION_ERROR";
    }

    public NotificationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NotificationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
