package com.marsh.pockets.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    Long userId
) {
}
