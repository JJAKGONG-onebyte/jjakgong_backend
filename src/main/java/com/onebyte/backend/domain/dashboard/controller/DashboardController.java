package com.onebyte.backend.domain.dashboard.controller;

import com.onebyte.backend.domain.dashboard.dto.DashboardResponse;
import com.onebyte.backend.domain.dashboard.service.DashboardService;
import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 홈 화면 통합 조회. date(yyyy-MM-dd)를 생략하면 오늘 기준 */
    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String date) {
        return ApiResponse.ok(dashboardService.getDashboard(userId, date));
    }
}
