package com.notification.notification.exception;

import com.google.common.util.concurrent.RateLimiter;
import com.notification.notification.dto.ErrorResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Global exception handler for the application with proper error handling,
 * sanitization, metrics, and consistent response structure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MeterRegistry meterRegistry;
    private final RateLimiter errorLogLimiter = RateLimiter.create(10.0); // 10 logs per second

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Sanitizes error messages to prevent information disclosure.
     */
    private String sanitizeErrorMessage(String message, String defaultMessage) {
        if (message == null || message.isEmpty()) {
            return defaultMessage;
        }

        // Remove potentially sensitive information
        String sanitized = message
            .replaceAll("[\r\n\t]", "_")
            .replaceAll("[\\p{Cntrl}]", "");

        // Check for sensitive keywords
        String lowerMessage = sanitized.toLowerCase();
        if (lowerMessage.contains("database") ||
            lowerMessage.contains("internal") ||
            lowerMessage.contains("sql") ||
            lowerMessage.contains("password") ||
            lowerMessage.contains("token")) {
            return defaultMessage;
        }

        // Limit length
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    /**
     * Logs with rate limiting to prevent log flooding.
     */
    private void logWithRateLimit(String message, Object... args) {
        if (errorLogLimiter.tryAcquire()) {
            logger.error(message, args);
        } else {
            logger.debug(message + " (rate limited)", args);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorId = UUID.randomUUID().toString();
        incrementErrorCounter("validation_error");

        StringBuilder fieldErrors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.append(error.getField())
                .append(": ")
                .append(error.getDefaultMessage())
                .append("; ")
        );

        logWithRateLimit("Validation error [{}]: {}", errorId, sanitizeErrorMessage(fieldErrors.toString(), "Validation failed"));

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed for request parameters",
            errorId
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(OrderNotFoundException ex) {
        String errorId = UUID.randomUUID().toString();
        incrementErrorCounter("order_not_found");

        logger.warn("Order not found [{}]: {}", errorId, sanitizeErrorMessage(ex.getMessage(), "Order not found"));

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            sanitizeErrorMessage(ex.getMessage(), "Order not found"),
            errorId
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String errorId = UUID.randomUUID().toString();
        incrementErrorCounter("illegal_argument");

        logger.warn("Invalid argument [{}]: {}", errorId, sanitizeErrorMessage(ex.getMessage(), "Invalid request"));

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            sanitizeErrorMessage(ex.getMessage(), "Invalid request parameter"),
            errorId
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ErrorResponse> handleOrderCreationException(OrderCreationException ex) {
        String errorId = UUID.randomUUID().toString();
        incrementErrorCounter("order_creation_failed");

        logWithRateLimit("Order creation failed [{}]: {}", errorId, ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Failed to create order. Please try again later.",
            errorId
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EventPublishingException.class)
    public ResponseEntity<ErrorResponse> handleEventPublishingException(EventPublishingException ex) {
        String errorId = UUID.randomUUID().toString();
        incrementErrorCounter("event_publishing_failed");

        logWithRateLimit("Event publishing failed [{}]: {}", errorId, ex.getMessage(), ex);

        // Return 202 ACCEPTED instead of 503, as the request was processed but notification failed
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.ACCEPTED.value(),
            "Request processed but event notification delayed",
            errorId
        );

        return new ResponseEntity<>(error, HttpStatus.ACCEPTED);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotificationException(NotificationException ex) {
        String errorId = UUID.randomUUID().toString();
        incrementErrorCounter("notification_failed");

        logWithRateLimit("Notification failed [{}]: {}", errorId, ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Notification service temporarily unavailable",
            errorId
        );

        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        incrementErrorCounter("unexpected_error");

        logger.error("Unexpected error [{}]: ", errorId, ex);

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred. Please contact support with error ID.",
            errorId
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Increments error counter for metrics.
     */
    private void incrementErrorCounter(String errorType) {
        Counter.builder("application.errors")
            .tag("type", errorType)
            .description("Number of errors by type")
            .register(meterRegistry)
            .increment();
    }
}

