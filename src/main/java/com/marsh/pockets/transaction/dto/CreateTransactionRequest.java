package com.marsh.pockets.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(
    @NotNull(message = "pocketId is required")
    Long pocketId,

    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    BigDecimal amount,

    String payeeUpiId,

    String note,

    @NotBlank(message = "idempotencyKey cannot be blank")
    String idempotencyKey,

    String rawQrPayload
) {
    public CreateTransactionRequest(Long pocketId, BigDecimal amount, String payeeUpiId, String note, String idempotencyKey) {
        this(pocketId, amount, payeeUpiId, note, idempotencyKey, null);
    }
}
