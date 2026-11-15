package com.onebyte.backend.domain.material.controller;

import com.onebyte.backend.domain.material.dto.SelectedMaterialRequest;
import com.onebyte.backend.domain.material.dto.SelectedMaterialResponse;
import com.onebyte.backend.domain.material.service.StudyMaterialService;
import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/selected-materials")
@RequiredArgsConstructor
public class SelectedMaterialController {

    private final StudyMaterialService studyMaterialService;

    @PutMapping
    public ApiResponse<SelectedMaterialResponse> select(
            @AuthenticationPrincipal Long userId,
            @RequestBody SelectedMaterialRequest request) {
        return ApiResponse.ok(studyMaterialService.select(userId, request.materialId()));
    }
}
