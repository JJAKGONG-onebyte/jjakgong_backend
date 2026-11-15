package com.onebyte.backend.domain.material.controller;

import com.onebyte.backend.domain.material.dto.MaterialListResponse;
import com.onebyte.backend.domain.material.dto.MaterialRenameRequest;
import com.onebyte.backend.domain.material.dto.MaterialRenameResponse;
import com.onebyte.backend.domain.material.dto.MaterialUploadResponse;
import com.onebyte.backend.domain.material.service.MaterialFile;
import com.onebyte.backend.domain.material.service.StudyMaterialService;
import com.onebyte.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    @GetMapping
    public ApiResponse<MaterialListResponse> getMaterials(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(studyMaterialService.getMaterials(userId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MaterialUploadResponse>> upload(
            @AuthenticationPrincipal Long userId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(studyMaterialService.upload(userId, file)));
    }

    @PatchMapping("/{materialId}/name")
    public ApiResponse<MaterialRenameResponse> rename(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long materialId,
            @Valid @RequestBody MaterialRenameRequest request) {
        return ApiResponse.ok(studyMaterialService.rename(userId, materialId, request.name()));
    }

    @DeleteMapping("/{materialId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long materialId) {
        studyMaterialService.delete(userId, materialId);
        return ApiResponse.ok();
    }

    /** 학습 화면의 "학습 자료" 탭에서 파일을 보여주기 위한 조회. Authorization 헤더가 필요하다. */
    @GetMapping("/{materialId}/file")
    public ResponseEntity<InputStreamResource> getFile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long materialId) {
        MaterialFile file = studyMaterialService.openFile(userId, materialId);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(file.stream()));
    }
}
