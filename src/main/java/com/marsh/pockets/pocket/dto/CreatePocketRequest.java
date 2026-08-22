package com.marsh.pockets.pocket.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePocketRequest(
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "monthlyLimit is required")
    @DecimalMin(value = "0.01", message = "monthlyLimit must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "monthlyLimit must have at most 15 integer digits and 4 decimal places")
    @DecimalMax(value = "999999999999999.9999", message = "monthlyLimit exceeds maximum allowed value")
    BigDecimal monthlyLimit
) {
}
