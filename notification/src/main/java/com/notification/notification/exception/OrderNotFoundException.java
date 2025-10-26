package com.notification.notification.exception;

/**
 * Exception thrown when an order is not found.
 * Used for 404 NOT FOUND responses.
 */
public class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super(orderId != null ?
            String.format("Order not found with ID: %d", orderId) :
            "Order not found with invalid ID: null");
    }

    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrderNotFoundException(Long orderId, Throwable cause) {
        super(String.format("Order not found with ID: %d", orderId), cause);
    }
}
