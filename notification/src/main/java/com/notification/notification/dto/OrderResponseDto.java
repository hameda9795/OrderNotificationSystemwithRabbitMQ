package com.notification.notification.dto;

import com.notification.notification.enums.OrderStatus;
import java.time.LocalDateTime;

/**
 * Response DTO for order information.
 */
public class OrderResponseDto {
    
    private Long id;
    private Long userId;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public OrderResponseDto() {
    }

    public OrderResponseDto(Long id, Long userId, OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
    }

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
}
