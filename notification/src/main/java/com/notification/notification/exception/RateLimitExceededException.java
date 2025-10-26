package com.notification.notification.exception;

/**
 * Exception thrown when external service rate limits are exceeded.
 * This represents a transient failure that requires backoff and retry.
 */
public class RateLimitExceededException extends NotificationException {

    private static final long serialVersionUID = 1L;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        if (retryAfterSeconds < 0) {
            throw new IllegalArgumentException("retryAfterSeconds must be non-negative, got: " + retryAfterSeconds);
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, Throwable cause, long retryAfterSeconds) {
        super(message, cause);
        if (retryAfterSeconds < 0) {
            throw new IllegalArgumentException("retryAfterSeconds must be non-negative, got: " + retryAfterSeconds);
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public String toString() {
        return super.toString() + ", retryAfterSeconds=" + retryAfterSeconds;
    }
}
