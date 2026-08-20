package com.marsh.pockets.payment.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateLinkRequest(
    @NotNull(message = "transactionId is required")
    Long transactionId
) {
}
