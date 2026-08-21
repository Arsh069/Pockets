package com.marsh.pockets.transaction.dto;

import com.marsh.pockets.transaction.entity.Transaction;
import com.marsh.pockets.transaction.entity.TransactionSource;
import com.marsh.pockets.transaction.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
    Long id,
    Long pocketId,
    Long userId,
    BigDecimal amount,
    String payeeUpiId,
    String payeeName,
    String note,
    String rawQrPayload,
    boolean amountLocked,
    TransactionStatus status,
    TransactionSource source,
    String idempotencyKey,
    Instant expiresAt,
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
            transaction.getPayeeName(),
            transaction.getNote(),
            transaction.getRawQrPayload(),
            transaction.isAmountLocked(),
            transaction.getStatus(),
            transaction.getSource(),
            transaction.getIdempotencyKey(),
            transaction.getExpiresAt(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
        );
    }
}
