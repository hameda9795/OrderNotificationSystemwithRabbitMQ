package com.notification.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.notification.event.OrderCreatedEvent;
import com.notification.notification.exception.NotificationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling notification operations.
 * Uses asynchronous processing and proper logging/monitoring.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public NotificationService(MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends email notification asynchronously.
     * @param userId The user ID
     * @param message The notification message
     * @return CompletableFuture for async processing
     */
    @Async
    @Retryable(
        retryFor = {NotificationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public CompletableFuture<Void> sendEmailNotification(Long userId, String message) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            logger.info("Sending email notification to user {} with message length: {}", userId, message.length());
            
            // Simulate email sending (replace with actual email service integration)
            // In production, this would call an email service like SendGrid, AWS SES, etc.
            
            sample.stop(meterRegistry.timer("notification.email.duration"));
            meterRegistry.counter("notification.email.success").increment();
            
            logger.info("Email notification sent successfully to user {}", userId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("notification.email.duration"));
            meterRegistry.counter("notification.email.failure").increment();
            logger.error("Failed to send email notification to user {}", userId, e);
            throw new NotificationException("Failed to send email notification", e);
        }
    }

    /**
     * Sends SMS notification asynchronously.
     * @param userId The user ID
     * @param message The notification message
     * @return CompletableFuture for async processing
     */
    @Async
    @Retryable(
        retryFor = {NotificationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public CompletableFuture<Void> sendSmsNotification(Long userId, String message) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            logger.info("Sending SMS notification to user {} with message: {}", userId, message);
            
            // Simulate SMS sending (replace with actual SMS service integration)
            // In production, this would call an SMS service like Twilio, AWS SNS, etc.
            
            sample.stop(meterRegistry.timer("notification.sms.duration"));
            meterRegistry.counter("notification.sms.success").increment();
            
            logger.info("SMS notification sent successfully to user {}", userId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("notification.sms.duration"));
            meterRegistry.counter("notification.sms.failure").increment();
            logger.error("Failed to send SMS notification to user {}", userId, e);
            throw new NotificationException("Failed to send SMS notification", e);
        }
    }

    /**
     * Handles order created events from RabbitMQ.
     * Uses proper message deserialization and error handling.
     * @param message The message from the queue
     */
    @RabbitListener(queues = "${app.rabbitmq.order-queue-name:order-queue}")
    public void handleOrderCreatedEvent(String message) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            logger.info("Received order created event: {}", message);
            
            // Deserialize the event from JSON
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            
            if (event.userId() == null) {
                logger.error("Invalid event: userId is null");
                meterRegistry.counter("notification.event.invalid").increment();
                return;
            }
            
            // Process notifications asynchronously
            String notificationMessage = String.format(
                "Your order %s has been created! Order ID: %d",
                event.orderNumber(),
                event.orderId()
            );
            
            sendEmailNotification(event.userId(), notificationMessage);
            sendSmsNotification(event.userId(), notificationMessage);
            
            sample.stop(meterRegistry.timer("notification.event.processing.duration"));
            meterRegistry.counter("notification.event.processed").increment();
            logger.info("Successfully processed order created event for order {}", event.orderId());
            
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("notification.event.processing.duration"));
            meterRegistry.counter("notification.event.failed").increment();
            logger.error("Failed to process order created event: {}", message, e);
            // In production, consider sending to a dead letter queue
        }
    }
}
