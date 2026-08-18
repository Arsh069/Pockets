package com.marsh.pockets.pocket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePocketRequest(
    @NotBlank(message = "Name cannot be blank")
    String name,

    @NotNull(message = "monthlyLimit is required")
    @DecimalMin(value = "0.01", message = "monthlyLimit must be greater than 0")
    BigDecimal monthlyLimit
) {
}
