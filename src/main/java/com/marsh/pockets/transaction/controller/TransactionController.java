package com.marsh.pockets.transaction.controller;

import com.marsh.pockets.transaction.dto.CreateTransactionRequest;
import com.marsh.pockets.transaction.dto.TransactionResponse;
import com.marsh.pockets.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/log")
    public ResponseEntity<TransactionResponse> logManualPurchase(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody com.marsh.pockets.transaction.dto.LogPurchaseRequest request) {
        TransactionResponse response = transactionService.logManualPurchase(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<TransactionResponse> confirmTransaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id) {
        TransactionResponse response = transactionService.confirmTransaction(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancelTransaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id) {
        TransactionResponse response = transactionService.cancelTransaction(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id) {
        TransactionResponse response = transactionService.getTransaction(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(name = "pocketId", required = false) Long pocketId) {
        List<TransactionResponse> responses = transactionService.listTransactions(userId, pocketId);
        return ResponseEntity.ok(responses);
    }
}
