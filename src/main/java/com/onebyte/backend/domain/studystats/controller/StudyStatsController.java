package com.onebyte.backend.domain.studystats.controller;

import com.onebyte.backend.domain.studystats.dto.StreakResponse;
import com.onebyte.backend.domain.studystats.dto.WeeklyStatsResponse;
import com.onebyte.backend.domain.studystats.service.StudyStatsService;
import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/stats")
@RequiredArgsConstructor
public class StudyStatsController {

    private final StudyStatsService studyStatsService;

    @GetMapping("/weekly")
    public ApiResponse<WeeklyStatsResponse> getWeekly(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(studyStatsService.getWeekly(userId));
    }

    @GetMapping("/streak")
    public ApiResponse<StreakResponse> getStreak(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(studyStatsService.getStreak(userId));
    }
}
