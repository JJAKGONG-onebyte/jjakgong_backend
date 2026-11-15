package com.onebyte.backend.domain.material.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.onebyte.backend.domain.material.entity.StudyMaterial;

import java.time.LocalDateTime;

public record MaterialUploadResponse(
        Long materialId,
        String name,
        String fileType,
        long fileSize,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime uploadedAt
) {
    public static MaterialUploadResponse from(StudyMaterial m) {
        return new MaterialUploadResponse(m.getId(), m.getName(), m.getFileType(), m.getFileSize(), m.getUploadedAt());
    }
}
