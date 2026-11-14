package com.onebyte.backend.domain.auth.dto;

import com.onebyte.backend.domain.user.entity.User;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record SignupResponse(Long userId, String email, String nickname, Long characterId, OffsetDateTime createdAt) {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getCharacter().getCharacterId(),
                user.getCreatedAt().atZone(KST).toOffsetDateTime());
    }
}
