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
    BigDecimal balance,
    BigDecimal overspentAmount,
    Instant createdAt,
    Instant updatedAt,
    Instant lastResetAt
) {
    public static PocketResponse fromEntity(Pocket pocket) {
        BigDecimal current = pocket.getCurrentBalance() != null ? pocket.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal manual = pocket.getManualCurrentBalance() != null ? pocket.getManualCurrentBalance() : BigDecimal.ZERO;

        BigDecimal balance = BigDecimal.ZERO.max(current);
        BigDecimal overspent = manual.compareTo(BigDecimal.ZERO) < 0 ? manual.abs() : BigDecimal.ZERO;

        return new PocketResponse(
            pocket.getId(),
            pocket.getUserId(),
            pocket.getName(),
            pocket.getMonthlyLimit(),
            current,
            manual,
            balance,
            overspent,
            pocket.getCreatedAt(),
            pocket.getUpdatedAt(),
            pocket.getLastResetAt()
        );
    }
}
