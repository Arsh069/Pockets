package com.marsh.pockets.payment.service;

import com.marsh.pockets.payment.dto.GenerateLinkResponse;

public interface PaymentLinkService {

    GenerateLinkResponse generateLink(Long transactionId, Long userId);
}
