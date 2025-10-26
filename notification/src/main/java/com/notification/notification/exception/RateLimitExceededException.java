package com.notification.notification.exception;

/**
 * Exception thrown when external service rate limits are exceeded.
 * This represents a transient failure that requires backoff and retry.
 */
public class RateLimitExceededException extends NotificationException {
    
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitExceededException(String message, Throwable cause, long retryAfterSeconds) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
