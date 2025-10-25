package com.notification.notification.controller;

import com.notification.notification.dto.CreateOrderRequest;
import com.notification.notification.dto.OrderResponseDto;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.notification.notification.entity.Order;
import com.notification.notification.service.OrderService;

/**
 * REST Controller for order management operations.
 * Follows REST best practices with proper validation, error handling, and documentation.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Creates a new order.
     * 
     * @param request The order creation request containing user ID
     * @return Response containing the created order details with 201 status
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Timed(value = "order.creation.time", description = "Time taken to create an order")
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        logger.info("Received request to create order for user {}", request.getUserId());
        
        try {
            Order order = orderService.createOrder(request.getUserId());
            
            OrderResponseDto response = new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getCreatedAt()
            );
            
            logger.info("Order created successfully: {}", order.getOrderNumber());
            
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for creating order: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error creating order for user {}", request.getUserId(), e);
            throw new RuntimeException("Failed to create order", e);
        }
    }
}

