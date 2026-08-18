package com.marsh.pockets.transaction.dto;

import com.marsh.pockets.transaction.entity.Transaction;
import com.marsh.pockets.transaction.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
    Long id,
    Long pocketId,
    Long userId,
    BigDecimal amount,
    String payeeUpiId,
    String note,
    TransactionStatus status,
    String idempotencyKey,
    Instant createdAt,
    Instant updatedAt
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getPocketId(),
            transaction.getUserId(),
            transaction.getAmount(),
            transaction.getPayeeUpiId(),
            transaction.getNote(),
            transaction.getStatus(),
            transaction.getIdempotencyKey(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
        );
    }
}
