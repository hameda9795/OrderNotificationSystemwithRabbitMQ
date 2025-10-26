package com.notification.notification.event;

import com.notification.notification.enums.OrderStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable event representing order creation.
 * Contains sufficient context for downstream services to process without additional database queries.
 */
public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    String orderNumber,
    OrderStatus status,
    LocalDateTime createdAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor with validation.
     */
    public OrderCreatedEvent {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(orderNumber, "Order number cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
    }
}