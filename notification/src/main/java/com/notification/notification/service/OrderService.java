package com.notification.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.notification.dto.OrderResponseDto;
import com.notification.notification.enums.OrderStatus;
import com.notification.notification.event.OrderCreatedEvent;
import com.notification.notification.exception.EventPublishingException;
import com.notification.notification.exception.OrderCreationException;
import com.notification.notification.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import com.notification.notification.repository.OrderRepository;
import com.notification.notification.entity.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Objects;
import java.util.Optional;

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
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${app.rabbitmq.order-queue-name:order-queue}")
    private String orderQueueName;
    
    public OrderService(OrderRepository orderRepository,
                        RabbitTemplate rabbitTemplate,
                        ObjectMapper objectMapper,
                        OrderMapper orderMapper,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Creates a new order and publishes an order created event.
     * The event is published after the transaction commits successfully.
     * Implements idempotency to prevent duplicate orders from network retries or user double-clicks.
     * 
     * @param userId The ID of the user creating the order
     * @param idempotencyKey The unique key to ensure idempotent order creation
     * @return OrderResponseDto containing the created order information
     * @throws IllegalArgumentException if userId is null or invalid, or if idempotencyKey is null
     * @throws OrderCreationException if order creation fails
     */
    @Transactional
    public OrderResponseDto createOrder(Long userId, String idempotencyKey) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        
        logger.info("Creating order for user {} with idempotency key {}", userId, idempotencyKey);
        
        // Check for existing order with same idempotency key
        Optional<Order> existingOrder = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingOrder.isPresent()) {
            logger.info("Returning existing order {} for idempotency key {}", 
                existingOrder.get().getOrderNumber(), idempotencyKey);
            return orderMapper.toDto(existingOrder.get());
        }
        
        Order order;
        try {
            // Create and save order
            order = new Order(userId, OrderStatus.CREATED, idempotencyKey);
            order = orderRepository.save(order);
            
            logger.info("Order created successfully: {}", order.getOrderNumber());
            
        } catch (DataAccessException e) {
            logger.error("Database error creating order for user {}", userId, e);
            throw new OrderCreationException("Database error occurred while creating order", e);
        } catch (Exception e) {
            logger.error("Unexpected error creating order for user {}", userId, e);
            throw new OrderCreationException("Failed to create order for user " + userId, e);
        }
        
        // Create and publish event to be sent after transaction commit
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getOrderNumber(),
            order.getStatus(),
            order.getCreatedAt()
        );
        
        // Publish event to Spring's ApplicationEventPublisher
        // The actual RabbitMQ message will be sent after transaction commits
        eventPublisher.publishEvent(event);
        
        logger.info("Order created event scheduled for publishing after transaction commit: {}", order.getOrderNumber());
        
        // Convert entity to DTO before returning
        return orderMapper.toDto(order);
    }
    
    /**
     * Handles the OrderCreatedEvent after the transaction commits successfully.
     * This ensures that the RabbitMQ message is only sent if the database transaction succeeds.
     * @param event The order created event to publish to RabbitMQ
     * @throws EventPublishingException if event publishing fails
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            // Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(orderQueueName, eventJson);
            
            logger.info("Order created event published to RabbitMQ for order {}", event.orderNumber());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order created event for order {}", event.orderNumber(), e);
            throw new EventPublishingException("Failed to serialize order event", e);
        } catch (AmqpException e) {
            logger.error("Messaging error for order {}", event.orderNumber(), e);
            // Order was created, but event failed - handle appropriately
            throw new EventPublishingException("Order created but notification failed", e);
        } catch (Exception e) {
            logger.error("Unexpected error publishing order created event to RabbitMQ for order {}", event.orderNumber(), e);
            throw new EventPublishingException("Failed to publish order event to message queue", e);
        }
    }
}
