package com.onebyte.backend.domain.studysetting.controller;

import com.onebyte.backend.domain.studysetting.dto.PomodoroRequest;
import com.onebyte.backend.domain.studysetting.dto.PomodoroResponse;
import com.onebyte.backend.domain.studysetting.dto.ScreenModeRequest;
import com.onebyte.backend.domain.studysetting.dto.ScreenModeResponse;
import com.onebyte.backend.domain.studysetting.dto.StudySettingResponse;
import com.onebyte.backend.domain.studysetting.dto.ToggleRequest;
import com.onebyte.backend.domain.studysetting.dto.TrackingStateResponse;
import com.onebyte.backend.domain.studysetting.service.StudySettingService;
import com.onebyte.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/settings")
@RequiredArgsConstructor
public class StudySettingController {

    private final StudySettingService studySettingService;

    @GetMapping
    public ApiResponse<StudySettingResponse> getSettings(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(studySettingService.getSettings(userId));
    }

    @PutMapping("/pomodoro")
    public ApiResponse<PomodoroResponse> updatePomodoro(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PomodoroRequest request) {
        return ApiResponse.ok(studySettingService.updatePomodoro(userId, request));
    }

    @PutMapping("/focus-tracking")
    public ApiResponse<TrackingStateResponse> updateFocusTracking(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ToggleRequest request) {
        return ApiResponse.ok(studySettingService.updateFocusTracking(userId, request));
    }

    @PutMapping("/webcam")
    public ApiResponse<TrackingStateResponse> updateWebcam(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ToggleRequest request) {
        return ApiResponse.ok(studySettingService.updateWebcam(userId, request));
    }

    @PutMapping("/screen-mode")
    public ApiResponse<ScreenModeResponse> updateScreenMode(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ScreenModeRequest request) {
        return ApiResponse.ok(studySettingService.updateScreenMode(userId, request));
    }
}
