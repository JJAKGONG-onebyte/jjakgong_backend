package com.onebyte.backend.domain.schedule.controller;

import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleDetailResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleListResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleUpdateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleUpdateResponse;
import com.onebyte.backend.domain.schedule.service.ScheduleService;
import com.onebyte.backend.domain.schedule.service.ScheduleUpdateService;
import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleUpdateService scheduleUpdateService;

    /** 일정 등록 (일반 / 반복) */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody ScheduleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(scheduleService.create(userId, request)));
    }

    /** 일정 목록 / 캘린더 조회. 오늘 일정은 startDate = endDate로 조회한다. */
    @GetMapping
    public ApiResponse<ScheduleListResponse> getList(
            @AuthenticationPrincipal Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ApiResponse.ok(scheduleService.getList(userId, startDate, endDate));
    }

    /** 일정 상세 조회. 반복 일정은 occurrenceDate 필수 */
    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleDetailResponse> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long scheduleId,
            @RequestParam(required = false) String occurrenceDate) {
        return ApiResponse.ok(scheduleService.getDetail(userId, scheduleId, occurrenceDate));
    }

    /**
     * 일정 부분 수정. 요청에 없는 필드는 유지하고, null을 보낸 time·deadlineDate는 삭제한다.
     * 반복 일정은 scope(THIS / ALL_FUTURE)와 occurrenceDate 필수, 일반 일정은 둘 다 생략한다.
     */
    @PatchMapping("/{scheduleId}")
    public ApiResponse<ScheduleUpdateResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long scheduleId,
            @RequestParam(required = false) String occurrenceDate,
            @RequestParam(required = false) String scope,
            @RequestBody ScheduleUpdateRequest request) {
        return ApiResponse.ok(scheduleUpdateService.update(userId, scheduleId, occurrenceDate, scope, request));
    }
}
