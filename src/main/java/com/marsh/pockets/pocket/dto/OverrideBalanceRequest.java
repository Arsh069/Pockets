package com.marsh.pockets.pocket.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OverrideBalanceRequest(
    @NotNull(message = "currentBalance is required")
    @DecimalMin(value = "0.00", message = "currentBalance must be greater than or equal to 0")
    @Digits(integer = 15, fraction = 4, message = "currentBalance must have at most 15 integer digits and 4 decimal places")
    @DecimalMax(value = "999999999999999.9999", message = "currentBalance exceeds maximum allowed value")
    BigDecimal currentBalance
) {
}
