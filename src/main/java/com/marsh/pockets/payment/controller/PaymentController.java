package com.marsh.pockets.payment.controller;

import com.marsh.pockets.payment.dto.GenerateLinkRequest;
import com.marsh.pockets.payment.dto.GenerateLinkResponse;
import com.marsh.pockets.payment.service.PaymentLinkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentLinkService paymentLinkService;

    public PaymentController(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    @PostMapping("/generate-link")
    public ResponseEntity<GenerateLinkResponse> generateLink(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GenerateLinkRequest request) {
        GenerateLinkResponse response = paymentLinkService.generateLink(request.transactionId(), userId);
        return ResponseEntity.ok(response);
    }
}
