package com.marsh.pockets.pocket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OverrideBalanceRequest(
    @NotNull(message = "currentBalance is required")
    @DecimalMin(value = "0.00", message = "currentBalance must be greater than or equal to 0")
    BigDecimal currentBalance
) {
}
