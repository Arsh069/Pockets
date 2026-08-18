package com.marsh.pockets.pocket.dto;

import java.math.BigDecimal;

public record BalanceCheckResponse(
    boolean sufficient,
    BigDecimal currentBalance
) {
}
