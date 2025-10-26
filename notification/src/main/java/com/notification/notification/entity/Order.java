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
@Table(name = "orders")
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
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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
        this.orderNumber = orderNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) && Objects.equals(orderNumber, order.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderNumber);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", userId=" + userId +
                ", status=" + status +
                ", orderNumber='" + orderNumber + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

