package com.notification.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Standard error response DTO to prevent sensitive information exposure.
 * Immutable record for thread safety and predictable error responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    LocalDateTime timestamp,

    @JsonProperty("status_code")
    int status,

    @JsonProperty("message")
    String message,

    @JsonProperty("error_id")
    String errorId
) {
    /**
     * Compact constructor with validation.
     */
    public ErrorResponse {
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(errorId, "Error ID cannot be null");

        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("Invalid HTTP status code: " + status);
        }

        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Factory method for bad request errors (400).
     */
    public static ErrorResponse badRequest(String message, String errorId) {
        return new ErrorResponse(LocalDateTime.now(), 400, message, errorId);
    }

    /**
     * Factory method for not found errors (404).
     */
    public static ErrorResponse notFound(String message, String errorId) {
        return new ErrorResponse(LocalDateTime.now(), 404, message, errorId);
    }

    /**
     * Factory method for internal server errors (500).
     */
    public static ErrorResponse internalServerError(String message, String errorId) {
        return new ErrorResponse(LocalDateTime.now(), 500, message, errorId);
    }

    /**
     * Factory method for service unavailable errors (503).
     */
    public static ErrorResponse serviceUnavailable(String message, String errorId) {
        return new ErrorResponse(LocalDateTime.now(), 503, message, errorId);
    }
}
