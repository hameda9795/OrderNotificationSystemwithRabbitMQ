package com.notification.notification.exception;

import java.util.Objects;

/**
 * Exception thrown when order creation fails due to business logic errors.
 * Supports error codes for categorizing different failure types.
 */
public class OrderCreationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public OrderCreationException(String message) {
        super(Objects.requireNonNull(message, "Error message cannot be null"));
        this.errorCode = "ORDER_CREATION_FAILED";
    }

    public OrderCreationException(String message, Throwable cause) {
        super(Objects.requireNonNull(message, "Error message cannot be null"), cause);
        this.errorCode = "ORDER_CREATION_FAILED";
    }

    public OrderCreationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OrderCreationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
