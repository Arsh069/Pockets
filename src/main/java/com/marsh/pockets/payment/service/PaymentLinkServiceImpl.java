package com.marsh.pockets.payment.service;

import com.marsh.pockets.common.exception.InvalidTransactionStateException;
import com.marsh.pockets.common.exception.ResourceNotFoundException;
import com.marsh.pockets.payment.dto.GenerateLinkResponse;
import com.marsh.pockets.transaction.entity.Transaction;
import com.marsh.pockets.transaction.entity.TransactionSource;
import com.marsh.pockets.transaction.entity.TransactionStatus;
import com.marsh.pockets.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

@Service
@Transactional(readOnly = true)
public class PaymentLinkServiceImpl implements PaymentLinkService {

    private final TransactionRepository transactionRepository;

    public PaymentLinkServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public GenerateLinkResponse generateLink(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found with id: " + transactionId);
        }

        if (transaction.getSource() != TransactionSource.IN_APP || transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(
                "UPI deep link can only be generated for PENDING IN_APP transactions. Current source: "
                + transaction.getSource() + ", status: " + transaction.getStatus()
            );
        }

        if (transaction.getPayeeUpiId() == null || transaction.getPayeeUpiId().trim().isEmpty()) {
            throw new IllegalArgumentException("payeeUpiId is required to generate a UPI deep link");
        }

        StringBuilder uriBuilder = new StringBuilder("upi://pay?");
        uriBuilder.append("pa=").append(URLEncoder.encode(transaction.getPayeeUpiId().trim(), StandardCharsets.UTF_8));
        if (transaction.getPayeeName() != null && !transaction.getPayeeName().trim().isEmpty()) {
            uriBuilder.append("&pn=").append(URLEncoder.encode(transaction.getPayeeName().trim(), StandardCharsets.UTF_8));
        }
        String amount = transaction.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        uriBuilder.append("&am=").append(URLEncoder.encode(amount, StandardCharsets.UTF_8));

        if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
            uriBuilder.append("&tn=").append(URLEncoder.encode(transaction.getNote().trim(), StandardCharsets.UTF_8));
        }

        uriBuilder.append("&cu=INR");
        uriBuilder.append("&tr=").append(URLEncoder.encode(transaction.getId().toString(), StandardCharsets.UTF_8));

        return new GenerateLinkResponse(uriBuilder.toString());
    }
}
