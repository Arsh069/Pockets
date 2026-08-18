package com.marsh.pockets.transaction.service;

import com.marsh.pockets.transaction.dto.CreateTransactionRequest;
import com.marsh.pockets.transaction.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(Long userId, CreateTransactionRequest request);

    TransactionResponse confirmTransaction(Long id, Long userId);

    TransactionResponse cancelTransaction(Long id, Long userId);

    TransactionResponse getTransaction(Long id, Long userId);

    List<TransactionResponse> listTransactions(Long userId, Long pocketId);
}
