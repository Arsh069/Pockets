package com.marsh.pockets.transaction.service;

import com.marsh.pockets.common.exception.InsufficientBalanceException;
import com.marsh.pockets.common.exception.InvalidAmountException;
import com.marsh.pockets.common.exception.InvalidTransactionStateException;
import com.marsh.pockets.common.exception.ResourceNotFoundException;
import com.marsh.pockets.pocket.dto.BalanceCheckResponse;
import com.marsh.pockets.pocket.service.PocketService;
import com.marsh.pockets.transaction.dto.CreateTransactionRequest;
import com.marsh.pockets.transaction.dto.TransactionResponse;
import com.marsh.pockets.transaction.entity.Transaction;
import com.marsh.pockets.transaction.entity.TransactionSource;
import com.marsh.pockets.transaction.entity.TransactionStatus;
import com.marsh.pockets.transaction.repository.TransactionRepository;
import com.marsh.pockets.transaction.util.UpiQrParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PocketService pocketService;
    private final long expiryMinutes;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            PocketService pocketService,
            @Value("${transaction.expiry-minutes:10}") long expiryMinutes) {
        this.transactionRepository = transactionRepository;
        this.pocketService = pocketService;
        this.expiryMinutes = expiryMinutes;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(Long userId, CreateTransactionRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Transaction existingTx = existing.get();
            if (!existingTx.getUserId().equals(userId)) {
                throw new ResourceNotFoundException("Transaction not found with idempotency key");
            }
            return TransactionResponse.fromEntity(existingTx);
        }

        if (transactionRepository.existsByPocketIdAndStatus(request.pocketId(), TransactionStatus.PENDING)) {
            throw new InvalidTransactionStateException("This pocket has a pending payment — confirm or cancel it first");
        }

        String resolvedPayeeUpiId;
        BigDecimal resolvedAmount;
        String resolvedPayeeName = null;
        boolean amountLocked = false;
        String rawQrPayload = request.rawQrPayload();

        /*
         * CRITICAL SECURITY RULE:
         * Client-supplied amount and payeeUpiId are NEVER trusted when a rawQrPayload is present.
         * A compromised or modified client could otherwise claim "amountLocked" in its own UI while
         * sending a different amount or redirecting payment to a rogue payee UPI ID in the request body,
         * without the backend having any way to detect the tampering.
         * Therefore, the server-parsed QR payload is the absolute authority for payment destination and locked amounts.
         */
        if (rawQrPayload != null && !rawQrPayload.trim().isEmpty()) {
            UpiQrParser.UpiQrPayload parsed = UpiQrParser.parse(rawQrPayload);
            resolvedPayeeUpiId = parsed.payeeUpiId();
            resolvedPayeeName = parsed.payeeName();

            if (parsed.amount() != null) {
                resolvedAmount = parsed.amount();
                amountLocked = true;
            } else {
                resolvedAmount = request.amount();
                amountLocked = false;
            }
        } else {
            resolvedPayeeUpiId = request.payeeUpiId();
            if (resolvedPayeeUpiId == null || !resolvedPayeeUpiId.matches("^[\\w.\\-]{2,256}@[a-zA-Z]{2,64}$")) {
                throw new IllegalArgumentException("payeeUpiId must be a valid UPI VPA format (e.g. name@bank)");
            }
            resolvedAmount = request.amount();
            amountLocked = false;
        }

        if (resolvedAmount == null || resolvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        // Verify the target pocket belongs to the authenticated user
        pocketService.getPocketById(request.pocketId(), userId);

        BalanceCheckResponse balanceCheck = pocketService.checkBalance(request.pocketId(), userId, resolvedAmount);
        if (!balanceCheck.sufficient()) {
            throw new InsufficientBalanceException(
                "Insufficient balance in pocket " + request.pocketId() + " for requested amount " + resolvedAmount
            );
        }

        Transaction transaction = new Transaction(
            request.pocketId(),
            userId,
            resolvedAmount,
            resolvedPayeeUpiId,
            request.note(),
            request.idempotencyKey()
        );
        transaction.setPayeeName(resolvedPayeeName);
        transaction.setRawQrPayload(rawQrPayload != null && !rawQrPayload.trim().isEmpty() ? rawQrPayload.trim() : null);
        transaction.setAmountLocked(amountLocked);
        transaction.setExpiresAt(Instant.now().plusSeconds(expiryMinutes * 60));
        transaction.setSource(TransactionSource.IN_APP);

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(noRollbackFor = {InvalidTransactionStateException.class, InsufficientBalanceException.class})
    public TransactionResponse confirmTransaction(Long id, Long userId) {
        Transaction transaction = findTransactionEntityAndVerifyOwnership(id, userId);

        // Check if PENDING transaction has expired
        if (transaction.getStatus() == TransactionStatus.PENDING
                && transaction.getExpiresAt() != null
                && transaction.getExpiresAt().isBefore(Instant.now())) {
            transaction.setStatus(TransactionStatus.EXPIRED);
            transactionRepository.save(transaction);
            throw new InvalidTransactionStateException("This payment request has expired, please create a new one");
        }

        if (transaction.getStatus() == TransactionStatus.EXPIRED) {
            throw new InvalidTransactionStateException("This payment request has expired, please create a new one");
        }

        if (transaction.getStatus() == TransactionStatus.CONFIRMED) {
            return TransactionResponse.fromEntity(transaction);
        }

        if (transaction.getStatus() == TransactionStatus.CANCELLED || transaction.getStatus() == TransactionStatus.FAILED) {
            throw new InvalidTransactionStateException(
                "Cannot confirm transaction with id " + id + " because current status is " + transaction.getStatus()
            );
        }

        try {
            pocketService.deductBalance(transaction.getPocketId(), transaction.getAmount());
            transaction.setStatus(TransactionStatus.CONFIRMED);
            Transaction saved = transactionRepository.save(transaction);
            return TransactionResponse.fromEntity(saved);
        } catch (InsufficientBalanceException ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionResponse cancelTransaction(Long id, Long userId) {
        Transaction transaction = findTransactionEntityAndVerifyOwnership(id, userId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(
                "Cannot cancel transaction with id " + id + " because current status is " + transaction.getStatus()
            );
        }

        if (transaction.getExpiresAt() != null && transaction.getExpiresAt().isBefore(Instant.now())) {
            transaction.setStatus(TransactionStatus.EXPIRED);
        } else {
            transaction.setStatus(TransactionStatus.CANCELLED);
        }

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    @Override
    public TransactionResponse getTransaction(Long id, Long userId) {
        Transaction transaction = findTransactionEntityAndVerifyOwnership(id, userId);
        return TransactionResponse.fromEntity(transaction);
    }

    @Override
    public List<TransactionResponse> listTransactions(Long userId, Long pocketId) {
        if (pocketId != null) {
            // Verify pocket ownership
            pocketService.getPocketById(pocketId, userId);
        }

        return transactionRepository.findAllFiltered(pocketId, userId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public TransactionResponse logManualPurchase(Long userId, com.marsh.pockets.transaction.dto.LogPurchaseRequest request) {
        if (request.amount() == null || request.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.marsh.pockets.common.exception.InvalidAmountException("Amount must be greater than zero");
        }

        // Verify the target pocket belongs to the authenticated user (throws ResourceNotFoundException if not)
        pocketService.getPocketById(request.pocketId(), userId);

        if (transactionRepository.existsByPocketIdAndStatus(request.pocketId(), TransactionStatus.PENDING)) {
            throw new InvalidTransactionStateException("This pocket has a pending payment — confirm or cancel it first");
        }

        // Update pocket balances via logManualDeduction (can drive manualCurrentBalance negative)
        pocketService.logManualDeduction(request.pocketId(), request.amount());

        // Create transaction with source = MANUAL_LOG, status = CONFIRMED directly
        Transaction transaction = new Transaction(
            request.pocketId(),
            userId,
            request.amount(),
            request.note(),
            com.marsh.pockets.transaction.entity.TransactionSource.MANUAL_LOG,
            TransactionStatus.CONFIRMED
        );

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    private Transaction findTransactionEntity(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    private Transaction findTransactionEntityAndVerifyOwnership(Long id, Long userId) {
        Transaction transaction = findTransactionEntity(id);
        if (!transaction.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found with id: " + id);
        }
        return transaction;
    }
}
