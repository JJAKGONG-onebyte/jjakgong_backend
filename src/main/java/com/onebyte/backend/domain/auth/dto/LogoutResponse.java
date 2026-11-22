package com.onebyte.backend.domain.auth.dto;

/** 로그아웃 응답 (ApiResponse의 data.message로 전달) */
public record LogoutResponse(String message) {

    private static final String DEFAULT_MESSAGE = "로그아웃되었습니다.";

    public static LogoutResponse success() {
        return new LogoutResponse(DEFAULT_MESSAGE);
    }
}
