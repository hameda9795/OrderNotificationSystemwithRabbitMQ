package com.notification.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.notification.notification.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * Response DTO for order information.
 * Immutable record ensuring thread safety and predictability.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponseDto(
    @NotNull
    @Positive
    @JsonProperty("id")
    Long id,

    @NotNull
    @Positive
    @JsonProperty("user_id")
    Long userId,

    @NotNull
    @JsonProperty("status")
    OrderStatus status,

    @NotNull
    @PastOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("created_at")
    LocalDateTime createdAt
) {}
