package com.marsh.pockets.pocket.dto;

import com.marsh.pockets.pocket.entity.Pocket;

import java.math.BigDecimal;
import java.time.Instant;

public record PocketResponse(
    Long id,
    Long userId,
    String name,
    BigDecimal monthlyLimit,
    BigDecimal currentBalance,
    BigDecimal manualCurrentBalance,
    BigDecimal displayBalance,
    Instant createdAt,
    Instant updatedAt,
    Instant lastResetAt,
    Instant lastManualOverrideAt
) {
    public static PocketResponse fromEntity(Pocket pocket) {
        BigDecimal current = pocket.getCurrentBalance();
        BigDecimal manual = pocket.getManualCurrentBalance();
        BigDecimal display = (current != null && manual != null) ? current.min(manual) : current;

        return new PocketResponse(
            pocket.getId(),
            pocket.getUserId(),
            pocket.getName(),
            pocket.getMonthlyLimit(),
            current,
            manual,
            display,
            pocket.getCreatedAt(),
            pocket.getUpdatedAt(),
            pocket.getLastResetAt(),
            pocket.getLastManualOverrideAt()
        );
    }
}
