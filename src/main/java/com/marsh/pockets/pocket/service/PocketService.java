package com.marsh.pockets.pocket.service;

import com.marsh.pockets.pocket.dto.BalanceCheckResponse;
import com.marsh.pockets.pocket.dto.CreatePocketRequest;
import com.marsh.pockets.pocket.dto.PocketResponse;
import com.marsh.pockets.pocket.dto.UpdatePocketRequest;

import java.math.BigDecimal;
import java.util.List;

public interface PocketService {

    PocketResponse createPocket(Long userId, CreatePocketRequest request);

    List<PocketResponse> getPocketsByUserId(Long userId);

    PocketResponse getPocketById(Long id, Long userId);

    PocketResponse updatePocket(Long id, Long userId, UpdatePocketRequest request);

    void deletePocket(Long id, Long userId);

    BalanceCheckResponse checkBalance(Long id, Long userId, BigDecimal amount);

    BalanceCheckResponse checkBalance(Long id, BigDecimal amount);

    void deductBalance(Long pocketId, BigDecimal amount);

    void resetBalance(Long pocketId);

    void resetBalance(Long pocketId, boolean ignoreOverrideProtection);

    PocketResponse resetBalanceForUser(Long pocketId, Long userId);

    List<PocketResponse> resetAllPocketsForUser(Long userId);

    PocketResponse overrideBalance(Long pocketId, Long userId, BigDecimal newBalance);
}
