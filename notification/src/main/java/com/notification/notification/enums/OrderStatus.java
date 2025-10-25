package com.notification.notification.enums;

/**
 * Enum representing the various states an order can be in.
 */
public enum OrderStatus {
    CREATED("Order has been created"),
    PENDING("Order is pending processing"),
    CONFIRMED("Order has been confirmed"),
    SHIPPED("Order has been shipped"),
    DELIVERED("Order has been delivered"),
    CANCELLED("Order has been cancelled");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
