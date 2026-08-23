package com.marsh.pockets.payment.dto;

import java.util.Map;

public record GenerateLinkResponse(
    String upiDeepLink,
    Map<String, String> iosAppLinks
) {
}
