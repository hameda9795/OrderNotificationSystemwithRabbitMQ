package com.notification.notification.exception;

/**
 * Exception thrown when publishing an event to the message queue fails.
 * Includes context about the event type and destination for better debugging.
 */
public class EventPublishingException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String eventType;
    private final String destination;

    public EventPublishingException(String message) {
        super(message);
        this.eventType = null;
        this.destination = null;
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
        this.eventType = null;
        this.destination = null;
    }

    public EventPublishingException(String message, String eventType, String destination) {
        super(message);
        this.eventType = eventType;
        this.destination = destination;
    }

    public EventPublishingException(String message, String eventType, String destination, Throwable cause) {
        super(message, cause);
        this.eventType = eventType;
        this.destination = destination;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDestination() {
        return destination;
    }
}
