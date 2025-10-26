package com.notification.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a new order.
 * Immutable record for better thread safety and predictability.
 */
public record CreateOrderRequest(
    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be positive")
    Long userId,

    @NotBlank(message = "Idempotency key cannot be blank")
    String idempotencyKey
) {
    /**
     * Compact constructor with additional validation.
     */
    public CreateOrderRequest {
        // Additional business validation if needed
        if (userId != null && userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (idempotencyKey != null && idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        }
    }
}
