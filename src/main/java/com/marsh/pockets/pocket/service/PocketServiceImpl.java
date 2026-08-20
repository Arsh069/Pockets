package com.marsh.pockets.pocket.service;

import com.marsh.pockets.common.exception.InsufficientBalanceException;
import com.marsh.pockets.common.exception.InvalidAmountException;
import com.marsh.pockets.common.exception.ResourceNotFoundException;
import com.marsh.pockets.pocket.dto.BalanceCheckResponse;
import com.marsh.pockets.pocket.dto.CreatePocketRequest;
import com.marsh.pockets.pocket.dto.PocketResponse;
import com.marsh.pockets.pocket.dto.UpdatePocketRequest;
import com.marsh.pockets.pocket.entity.Pocket;
import com.marsh.pockets.pocket.repository.PocketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PocketServiceImpl implements PocketService {

    private final PocketRepository pocketRepository;

    public PocketServiceImpl(PocketRepository pocketRepository) {
        this.pocketRepository = pocketRepository;
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
        pocket.setName(request.name());
        pocket.setMonthlyLimit(request.monthlyLimit());
        Pocket updated = pocketRepository.save(pocket);
        return PocketResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deletePocket(Long id, Long userId) {
        Pocket pocket = findPocketEntityAndVerifyOwnership(id, userId);
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
        resetBalance(pocketId, false);
    }

    @Override
    @Transactional
    public void resetBalance(Long pocketId, boolean ignoreOverrideProtection) {
        Pocket pocket = findPocketEntity(pocketId);
        Instant now = Instant.now();

        boolean isOverrideProtected = !ignoreOverrideProtection
                && pocket.getLastManualOverrideAt() != null
                && (pocket.getLastResetAt() == null || pocket.getLastManualOverrideAt().isAfter(pocket.getLastResetAt()));

        // The scheduled job must be idempotent because it can be triggered more
        // than once on the same day. Manual reset requests are allowed to run
        // again immediately so the override-expiration flow can complete.
        if (ignoreOverrideProtection && pocket.getLastResetAt() != null) {
            LocalDate lastResetDate = LocalDate.ofInstant(pocket.getLastResetAt(), ZoneId.systemDefault());
            LocalDate todayDate = LocalDate.ofInstant(now, ZoneId.systemDefault());
            if (lastResetDate.equals(todayDate)) {
                return;
            }
        }

        // Manual Reset Behavior vs Scheduled Reset Behavior:
        // When ignoreOverrideProtection = false (manual resets):
        // If a user manually edited their balance (lastManualOverrideAt != null) AND that edit occurred AFTER the last reset,
        // we DO NOT overwrite currentBalance or manualCurrentBalance. However, we STILL update lastResetAt = now.
        // EXPIRATION MECHANISM:
        // Updating lastResetAt to now ensures that during the NEXT reset cycle, lastResetAt (now) will be AFTER lastManualOverrideAt,
        // causing `lastManualOverrideAt.isAfter(lastResetAt)` to evaluate to false. Thus, override protection automatically expires after one cycle.
        if (!isOverrideProtected) {
            pocket.setCurrentBalance(pocket.getMonthlyLimit());
            pocket.setManualCurrentBalance(pocket.getMonthlyLimit());
        }

        pocket.setLastResetAt(now);
        pocketRepository.save(pocket);
    }

    @Override
    @Transactional
    public PocketResponse resetBalanceForUser(Long pocketId, Long userId) {
        Pocket pocket = findPocketEntityAndVerifyOwnership(pocketId, userId);
        resetBalance(pocket.getId(), false);
        return PocketResponse.fromEntity(findPocketEntity(pocketId));
    }

    @Override
    @Transactional
    public List<PocketResponse> resetAllPocketsForUser(Long userId) {
        List<Pocket> pockets = pocketRepository.findByUserId(userId);
        for (Pocket pocket : pockets) {
            resetBalance(pocket.getId(), false);
        }
        return pocketRepository.findByUserId(userId)
                .stream()
                .map(PocketResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public PocketResponse overrideBalance(Long pocketId, Long userId, BigDecimal newBalance) {
        if (newBalance == null || newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("currentBalance must be greater than or equal to 0");
        }

        Pocket pocket = findPocketEntityAndVerifyOwnership(pocketId, userId);
        pocket.setCurrentBalance(newBalance);
        pocket.setManualCurrentBalance(newBalance);
        pocket.setLastManualOverrideAt(Instant.now());
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
