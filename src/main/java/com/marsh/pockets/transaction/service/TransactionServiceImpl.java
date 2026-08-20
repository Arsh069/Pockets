package com.marsh.pockets.transaction.service;

import com.marsh.pockets.common.exception.InsufficientBalanceException;
import com.marsh.pockets.common.exception.InvalidTransactionStateException;
import com.marsh.pockets.common.exception.ResourceNotFoundException;
import com.marsh.pockets.pocket.dto.BalanceCheckResponse;
import com.marsh.pockets.pocket.service.PocketService;
import com.marsh.pockets.transaction.dto.CreateTransactionRequest;
import com.marsh.pockets.transaction.dto.TransactionResponse;
import com.marsh.pockets.transaction.entity.Transaction;
import com.marsh.pockets.transaction.entity.TransactionStatus;
import com.marsh.pockets.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PocketService pocketService;

    public TransactionServiceImpl(TransactionRepository transactionRepository, PocketService pocketService) {
        this.transactionRepository = transactionRepository;
        this.pocketService = pocketService;
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

        // Verify the target pocket belongs to the authenticated user
        pocketService.getPocketById(request.pocketId(), userId);

        BalanceCheckResponse balanceCheck = pocketService.checkBalance(request.pocketId(), userId, request.amount());
        if (!balanceCheck.sufficient()) {
            throw new InsufficientBalanceException(
                "Insufficient balance in pocket " + request.pocketId() + " for requested amount " + request.amount()
            );
        }

        Transaction transaction = new Transaction(
            request.pocketId(),
            userId,
            request.amount(),
            request.payeeUpiId(),
            request.note(),
            request.idempotencyKey()
        );

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public TransactionResponse confirmTransaction(Long id, Long userId) {
        Transaction transaction = findTransactionEntityAndVerifyOwnership(id, userId);

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

        transaction.setStatus(TransactionStatus.CANCELLED);
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
