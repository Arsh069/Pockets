package com.marsh.pockets.transaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LogPurchaseRequest(
    @NotNull(message = "pocketId is required")
    Long pocketId,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "amount must have at most 15 integer digits and 4 decimal places")
    @DecimalMax(value = "999999999999999.9999", message = "amount exceeds maximum allowed value")
    BigDecimal amount,

    @Size(max = 255, message = "Note must not exceed 255 characters")
    String note
) {
}
