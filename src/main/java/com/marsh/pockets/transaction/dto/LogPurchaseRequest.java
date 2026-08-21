package com.marsh.pockets.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LogPurchaseRequest(
    @NotNull(message = "pocketId is required")
    Long pocketId,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    BigDecimal amount,

    String note
) {
}
