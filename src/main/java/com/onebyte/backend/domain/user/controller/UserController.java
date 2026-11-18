package com.onebyte.backend.domain.user.controller;

import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.onebyte.backend.domain.user.dto.ChangePasswordRequest;
import com.onebyte.backend.domain.user.dto.ProfileResponse;
import com.onebyte.backend.domain.user.dto.UpdateProfileRequest;
import com.onebyte.backend.domain.user.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PatchMapping
    public ApiResponse<ProfileResponse> updateProfile(@AuthenticationPrincipal Long userId,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(userId, request));
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Long userId,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ApiResponse.ok();
    }
}
