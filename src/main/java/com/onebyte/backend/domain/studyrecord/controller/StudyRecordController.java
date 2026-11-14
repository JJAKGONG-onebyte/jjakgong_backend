package com.onebyte.backend.domain.studyrecord.controller;

import com.onebyte.backend.domain.studyrecord.dto.StudyRecordPageResponse;
import com.onebyte.backend.domain.studyrecord.dto.StudyRecordPeriod;
import com.onebyte.backend.domain.studyrecord.service.StudyRecordService;
import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/records")
@RequiredArgsConstructor
public class StudyRecordController {

    private final StudyRecordService studyRecordService;

    @GetMapping
    public ApiResponse<StudyRecordPageResponse> getRecords(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "WEEK") StudyRecordPeriod period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(studyRecordService.getRecords(userId, period, page, size));
    }
}
