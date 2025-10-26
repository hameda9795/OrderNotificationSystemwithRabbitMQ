package com.notification.notification.mapper;

import com.notification.notification.dto.OrderResponseDto;
import com.notification.notification.entity.Order;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Order entity and DTOs.
 * This ensures proper separation between domain model and API contract.
 */
@Component
public class OrderMapper {
    
    /**
     * Converts an Order entity to OrderResponseDto.
     * 
     * @param order The order entity to convert
     * @return OrderResponseDto containing the order information
     * @throws IllegalArgumentException if order is null
     */
    public OrderResponseDto toDto(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        return new OrderResponseDto(
            order.getId(),
            order.getUserId(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
