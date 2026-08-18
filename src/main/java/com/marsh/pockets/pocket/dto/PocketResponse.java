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
    Instant createdAt,
    Instant updatedAt
) {
    public static PocketResponse fromEntity(Pocket pocket) {
        return new PocketResponse(
            pocket.getId(),
            pocket.getUserId(),
            pocket.getName(),
            pocket.getMonthlyLimit(),
            pocket.getCurrentBalance(),
            pocket.getCreatedAt(),
            pocket.getUpdatedAt()
        );
    }
}
