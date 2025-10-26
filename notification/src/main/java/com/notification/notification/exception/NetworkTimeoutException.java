package com.notification.notification.exception;

/**
 * Exception thrown when a network timeout occurs while communicating with external services.
 * This represents a transient failure that may succeed on retry.
 *
 * <p>This exception should be thrown when:
 * <ul>
 *   <li>HTTP client timeout occurs (connection or read timeout)</li>
 *   <li>Circuit breaker timeout is triggered</li>
 *   <li>External service doesn't respond within configured SLA</li>
 * </ul>
 *
 * @since 1.0
 * @see NotificationException
 */
public class NetworkTimeoutException extends NotificationException {

    private static final long serialVersionUID = 1L;

    public NetworkTimeoutException(String message) {
        super(message);
    }

    public NetworkTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
