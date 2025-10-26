package com.notification.notification.exception;

/**
 * Exception thrown when recipient data is invalid or malformed.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Malformed email addresses (missing @, invalid domain)</li>
 *   <li>Invalid phone numbers (wrong format, missing country code)</li>
 *   <li>Empty or null recipient identifiers</li>
 * </ul>
 *
 * <p>This represents a non-retryable client error (4xx) that requires
 * correction of input data before retry.
 *
 * @see NotificationException
 * @since 1.0
 */
public class InvalidRecipientException extends NotificationException {

    private static final long serialVersionUID = 1L;

    public InvalidRecipientException(String message) {
        super(message);
    }

    public InvalidRecipientException(String message, Throwable cause) {
        super(message, cause);
    }
}
