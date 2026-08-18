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
    public void resetBalance(Long pocketId) {
        throw new UnsupportedOperationException("Monthly reset job functionality will be implemented in a future step.");
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
