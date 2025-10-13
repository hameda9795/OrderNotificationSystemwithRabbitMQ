package com.notification.notification.event;

public class OrderCreatedEvent {
    
    private Long userId;
    
    public OrderCreatedEvent(Long userId) {
        this.userId = userId;
    }
    
    public Long getUserId() {
        return userId;
    }
}