package com.marsh.pockets.transaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateTransactionRequest(
    @NotNull(message = "pocketId is required")
    Long pocketId,

    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "amount must have at most 15 integer digits and 4 decimal places")
    @DecimalMax(value = "999999999999999.9999", message = "amount exceeds maximum allowed value")
    BigDecimal amount,

    @Pattern(regexp = "^[\\w.\\-]{2,256}@[a-zA-Z]{2,64}$", message = "payeeUpiId must be a valid UPI VPA format (e.g. name@bank)")
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
