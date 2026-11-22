package com.onebyte.backend.domain.auth.controller;

import com.onebyte.backend.domain.auth.dto.LogoutResponse;
import com.onebyte.backend.domain.auth.service.LogoutService;
import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그아웃 (인증 필요, SecurityConfig에서 /api/auth/logout은 permitAll 대상이 아님) */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LogoutController {

    private final LogoutService logoutService;

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(@AuthenticationPrincipal Long userId) {
        logoutService.logout(userId);
        return ApiResponse.ok(LogoutResponse.success());
    }
}
