package com.marsh.pockets.pocket.service;

import com.marsh.pockets.common.exception.InsufficientBalanceException;
import com.marsh.pockets.common.exception.InvalidAmountException;
import com.marsh.pockets.common.exception.InvalidTransactionStateException;
import com.marsh.pockets.common.exception.ResourceNotFoundException;
import com.marsh.pockets.pocket.dto.BalanceCheckResponse;
import com.marsh.pockets.pocket.dto.CreatePocketRequest;
import com.marsh.pockets.pocket.dto.PocketResponse;
import com.marsh.pockets.pocket.dto.UpdatePocketRequest;
import com.marsh.pockets.pocket.entity.Pocket;
import com.marsh.pockets.pocket.repository.PocketRepository;
import com.marsh.pockets.transaction.entity.TransactionStatus;
import com.marsh.pockets.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PocketServiceImpl implements PocketService {

    private static final Logger log = LoggerFactory.getLogger(PocketServiceImpl.class);

    private final PocketRepository pocketRepository;
    private final TransactionRepository transactionRepository;

    public PocketServiceImpl(PocketRepository pocketRepository, TransactionRepository transactionRepository) {
        this.pocketRepository = pocketRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public PocketResponse createPocket(Long userId, CreatePocketRequest request) {
        Pocket pocket = new Pocket(userId, request.name(), request.monthlyLimit());
        Pocket saved = pocketRepository.save(pocket);
        return PocketResponse.fromEntity(saved);
    }

    @Override
    public List<PocketResponse> getPocketsByUserId(Long userId) {
        return pocketRepository.findByUserId(userId)
                .stream()
                .map(PocketResponse::fromEntity)
                .toList();
    }

    @Override
    public PocketResponse getPocketById(Long id, Long userId) {
        Pocket pocket = findPocketEntityAndVerifyOwnership(id, userId);
        return PocketResponse.fromEntity(pocket);
    }

    @Override
    @Transactional
    public PocketResponse updatePocket(Long id, Long userId, UpdatePocketRequest request) {
        Pocket pocket = findPocketEntityAndVerifyOwnership(id, userId);

        if (transactionRepository.existsByPocketIdAndStatus(id, TransactionStatus.PENDING)) {
            throw new InvalidTransactionStateException("This pocket has a pending payment — confirm or cancel it first");
        }

        pocket.setName(request.name());
        pocket.setMonthlyLimit(request.monthlyLimit());
        Pocket updated = pocketRepository.save(pocket);
        return PocketResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deletePocket(Long id, Long userId) {
        Pocket pocket = findPocketEntityAndVerifyOwnership(id, userId);

        if (transactionRepository.existsByPocketIdAndStatus(id, TransactionStatus.PENDING)) {
            throw new InvalidTransactionStateException("This pocket has a pending payment — confirm or cancel it first");
        }

        pocketRepository.delete(pocket);
    }

    @Override
    public BalanceCheckResponse checkBalance(Long id, Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        Pocket pocket = findPocketEntityAndVerifyOwnership(id, userId);
        boolean sufficient = pocket.getCurrentBalance().compareTo(amount) >= 0;
        return new BalanceCheckResponse(sufficient, pocket.getCurrentBalance());
    }

    @Override
    public BalanceCheckResponse checkBalance(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        Pocket pocket = findPocketEntity(id);
        boolean sufficient = pocket.getCurrentBalance().compareTo(amount) >= 0;
        return new BalanceCheckResponse(sufficient, pocket.getCurrentBalance());
    }

    @Override
    @Transactional
    public void deductBalance(Long pocketId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (!pocketRepository.existsById(pocketId)) {
            throw new ResourceNotFoundException("Pocket not found with id: " + pocketId);
        }

        int updatedRows = pocketRepository.deductBalanceAtomic(pocketId, amount);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException(
                "Insufficient balance in pocket with id: " + pocketId + " for amount: " + amount
            );
        }
    }

    @Override
    @Transactional
    public void logManualDeduction(Long pocketId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        Pocket pocket = findPocketEntity(pocketId);

        // Deduct from manualCurrentBalance directly (can go negative)
        BigDecimal currentManual = pocket.getManualCurrentBalance() != null ? pocket.getManualCurrentBalance() : BigDecimal.ZERO;
        pocket.setManualCurrentBalance(currentManual.subtract(amount));

        // Deduct from currentBalance, clamping at 0 to respect DB check constraint
        BigDecimal current = pocket.getCurrentBalance() != null ? pocket.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newCurrent = current.subtract(amount);
        if (newCurrent.compareTo(BigDecimal.ZERO) < 0) {
            newCurrent = BigDecimal.ZERO;
        }
        pocket.setCurrentBalance(newCurrent);

        pocketRepository.save(pocket);
    }

    @Override
    @Transactional
    public void resetBalance(Long pocketId) {
        Pocket pocket = findPocketEntity(pocketId);
        Instant now = Instant.now();

        pocket.setCurrentBalance(pocket.getMonthlyLimit());
        pocket.setManualCurrentBalance(pocket.getMonthlyLimit());
        pocket.setLastResetAt(now);
        pocketRepository.save(pocket);
    }

    @Override
    @Transactional
    public PocketResponse resetBalanceForUser(Long pocketId, Long userId) {
        Pocket pocket = findPocketEntityAndVerifyOwnership(pocketId, userId);

        if (transactionRepository.existsByPocketIdAndStatus(pocketId, TransactionStatus.PENDING)) {
            throw new InvalidTransactionStateException("This pocket has a pending payment — confirm or cancel it first");
        }

        resetBalance(pocket.getId());
        return PocketResponse.fromEntity(findPocketEntity(pocketId));
    }

    @Override
    @Transactional
    public List<PocketResponse> resetAllPocketsForUser(Long userId) {
        List<Pocket> pockets = pocketRepository.findByUserId(userId);
        int skippedCount = 0;
        for (Pocket pocket : pockets) {
            if (transactionRepository.existsByPocketIdAndStatus(pocket.getId(), TransactionStatus.PENDING)) {
                log.info("Skipping reset for pocket {} as it has a PENDING transaction", pocket.getId());
                skippedCount++;
                continue;
            }
            resetBalance(pocket.getId());
        }
        log.info("Reset all pockets for user {}: total = {}, skipped with pending = {}", userId, pockets.size(), skippedCount);

        return pocketRepository.findByUserId(userId)
                .stream()
                .map(PocketResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public PocketResponse overrideBalance(Long pocketId, Long userId, BigDecimal newBalance) {
        Pocket pocket = findPocketEntityAndVerifyOwnership(pocketId, userId);

        if (transactionRepository.existsByPocketIdAndStatus(pocketId, TransactionStatus.PENDING)) {
            throw new InvalidTransactionStateException("This pocket has a pending payment — confirm or cancel it first");
        }

        if (newBalance == null || newBalance.compareTo(BigDecimal.ZERO) < 0 || newBalance.compareTo(pocket.getMonthlyLimit()) > 0) {
            String limitStr = pocket.getMonthlyLimit().stripTrailingZeros().toPlainString();
            throw new IllegalArgumentException("Balance must be between 0 and the monthly limit (₹" + limitStr + ")");
        }

        pocket.setCurrentBalance(newBalance);
        pocket.setManualCurrentBalance(newBalance);
        Pocket saved = pocketRepository.save(pocket);
        return PocketResponse.fromEntity(saved);
    }

    private Pocket findPocketEntity(Long id) {
        return pocketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pocket not found with id: " + id));
    }

    private Pocket findPocketEntityAndVerifyOwnership(Long id, Long userId) {
        Pocket pocket = findPocketEntity(id);
        if (!pocket.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Pocket not found with id: " + id);
        }
        return pocket;
    }
}
