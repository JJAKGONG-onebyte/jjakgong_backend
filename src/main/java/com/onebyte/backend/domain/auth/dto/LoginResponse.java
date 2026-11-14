package com.onebyte.backend.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String nickname
) {
}
