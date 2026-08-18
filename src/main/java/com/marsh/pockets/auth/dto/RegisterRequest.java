package com.marsh.pockets.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "phoneNumber must be a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9")
    String phoneNumber,

    @NotBlank(message = "password is required")
    String password,

    String name,

    @NotNull(message = "payDay is required")
    @Min(value = 1, message = "payDay must be between 1 and 31")
    @Max(value = 31, message = "payDay must be between 1 and 31")
    Integer payDay
) {
}
