package com.marsh.pockets.pocket.controller;

import com.marsh.pockets.pocket.dto.BalanceCheckResponse;
import com.marsh.pockets.pocket.dto.CreatePocketRequest;
import com.marsh.pockets.pocket.dto.OverrideBalanceRequest;
import com.marsh.pockets.pocket.dto.PocketResponse;
import com.marsh.pockets.pocket.dto.UpdatePocketRequest;
import com.marsh.pockets.pocket.service.PocketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pockets")
public class PocketController {

    private final PocketService pocketService;

    public PocketController(PocketService pocketService) {
        this.pocketService = pocketService;
    }

    @PostMapping
    public ResponseEntity<PocketResponse> createPocket(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePocketRequest request) {
        PocketResponse response = pocketService.createPocket(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PocketResponse>> getPocketsByUserId(@AuthenticationPrincipal Long userId) {
        List<PocketResponse> responses = pocketService.getPocketsByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PocketResponse> getPocketById(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id) {
        PocketResponse response = pocketService.getPocketById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PocketResponse> updatePocket(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePocketRequest request) {
        PocketResponse response = pocketService.updatePocket(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePocket(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id) {
        pocketService.deletePocket(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/check")
    public ResponseEntity<BalanceCheckResponse> checkBalance(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id,
            @RequestParam("amount") BigDecimal amount) {
        BalanceCheckResponse response = pocketService.checkBalance(id, userId, amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<PocketResponse> resetPocket(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id) {
        PocketResponse response = pocketService.resetBalanceForUser(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-all")
    public ResponseEntity<List<PocketResponse>> resetAllPockets(@AuthenticationPrincipal Long userId) {
        List<PocketResponse> responses = pocketService.resetAllPocketsForUser(userId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/balance")
    public ResponseEntity<PocketResponse> overrideBalance(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id,
            @Valid @RequestBody OverrideBalanceRequest request) {
        PocketResponse response = pocketService.overrideBalance(id, userId, request.currentBalance());
        return ResponseEntity.ok(response);
    }
}
