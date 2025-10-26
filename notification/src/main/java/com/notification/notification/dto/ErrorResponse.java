package com.notification.notification.dto;

import java.time.LocalDateTime;

/**
 * Standard error response DTO to prevent sensitive information exposure.
 */
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String errorId;

    private ErrorResponse(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status = builder.status;
        this.message = builder.message;
        this.errorId = builder.errorId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorId() {
        return errorId;
    }

    public static class Builder {
        private LocalDateTime timestamp;
        private int status;
        private String message;
        private String errorId;

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorId(String errorId) {
            this.errorId = errorId;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
}
