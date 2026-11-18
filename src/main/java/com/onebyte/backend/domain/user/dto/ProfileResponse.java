package com.onebyte.backend.domain.user.dto;

import com.onebyte.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record ProfileResponse(Long userId, String email, String nickname, LocalDateTime createdAt) {

    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
    }
}
