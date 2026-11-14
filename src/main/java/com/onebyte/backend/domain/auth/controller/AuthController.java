package com.onebyte.backend.domain.auth.controller;

import com.onebyte.backend.domain.auth.dto.LoginRequest;
import com.onebyte.backend.domain.auth.dto.LoginResponse;
import com.onebyte.backend.domain.auth.dto.PasswordResetConfirmRequest;
import com.onebyte.backend.domain.auth.dto.PasswordResetSendRequest;
import com.onebyte.backend.domain.auth.dto.PasswordResetSendResponse;
import com.onebyte.backend.domain.auth.dto.RefreshRequest;
import com.onebyte.backend.domain.auth.dto.SignupRequest;
import com.onebyte.backend.domain.auth.dto.SignupResponse;
import com.onebyte.backend.domain.auth.dto.TokenResponse;
import com.onebyte.backend.domain.auth.service.AuthService;
import com.onebyte.backend.domain.auth.service.PasswordResetService;
import com.onebyte.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.signup(request)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/pw-reset/send")
    public ApiResponse<PasswordResetSendResponse> sendPasswordResetCode(
            @Valid @RequestBody PasswordResetSendRequest request) {
        return ApiResponse.ok(passwordResetService.send(request));
    }

    @PostMapping("/pw-reset/confirm")
    public ApiResponse<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request);
        return ApiResponse.ok();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }
}
