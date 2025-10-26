package com.notification.notification.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.notification.notification.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entity representing an Order in the system.
 * Uses JPA auditing for automatic tracking of creation and modification timestamps.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "userId"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "createdAt"),
    @Index(name = "idx_order_user_status", columnList = "userId, status"),
    @Index(name = "idx_order_number", columnList = "orderNumber", unique = true),
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(unique = true, nullable = false, length = 100)
    private String orderNumber;

    @Column(unique = true, nullable = false, length = 255)
    private String idempotencyKey;

    /**
     * Default constructor for JPA.
     */
    public Order() {
    }

    /**
     * Constructor with required fields.
     * @param userId The ID of the user who created the order
     * @param status The initial status of the order
     */
    public Order(Long userId, OrderStatus status) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.orderNumber = generateOrderNumber();
    }

    /**
     * Constructor with required fields including idempotency key.
     * @param userId The ID of the user who created the order
     * @param status The initial status of the order
     * @param idempotencyKey The idempotency key to prevent duplicate orders
     */
    public Order(Long userId, OrderStatus status, String idempotencyKey) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.orderNumber = generateOrderNumber();
    }

    /**
     * Generates a unique order number using UUID.
     * @return A unique order number
     */
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString();
    }

    /**
     * Updates the order status.
     * @param newStatus The new status
     */
    public void updateStatus(OrderStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "Status cannot be null");
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        if (orderNumber != null && !orderNumber.matches("^ORD-[a-fA-F0-9-]{36}$")) {
            throw new IllegalArgumentException("Invalid order number format. Expected: ORD-{UUID}");
        }
        this.orderNumber = orderNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        // Use orderNumber primarily, fall back to id if both are persisted
        return Objects.equals(orderNumber, order.orderNumber) ||
               (Objects.equals(id, order.id) && id != null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderNumber);
    }

    @Override
    public String toString() {
        // Exclude userId from toString to prevent accidental logging of sensitive data
        return "Order{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

