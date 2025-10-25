package com.notification.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.notification.enums.OrderStatus;
import com.notification.notification.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.notification.notification.repository.OrderRepository;
import com.notification.notification.entity.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Service for managing order operations.
 * Uses transaction management and proper event publishing.
 */
@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.rabbitmq.order-queue-name:order-queue}")
    private String orderQueueName;
    
    public OrderService(OrderRepository orderRepository,
                        RabbitTemplate rabbitTemplate,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Creates a new order and publishes an order created event.
     * @param userId The ID of the user creating the order
     * @return The created order
     * @throws IllegalArgumentException if userId is null or invalid
     */
    @Transactional
    public Order createOrder(Long userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        
        logger.info("Creating order for user {}", userId);
        
        try {
            // Create and save order
            Order order = new Order(userId, OrderStatus.CREATED);
            order = orderRepository.save(order);
            
            logger.info("Order created successfully: {}", order.getOrderNumber());
            
            // Create and publish event
            OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCreatedAt()
            );
            
            // Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(orderQueueName, eventJson);
            
            logger.info("Order created event published for order {}", order.getOrderNumber());
            
            return order;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order created event", e);
            throw new RuntimeException("Failed to publish order event", e);
        } catch (Exception e) {
            logger.error("Failed to create order for user {}", userId, e);
            throw new RuntimeException("Failed to create order", e);
        }
    }
}
