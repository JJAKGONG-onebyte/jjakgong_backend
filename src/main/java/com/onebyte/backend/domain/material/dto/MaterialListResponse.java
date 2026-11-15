package com.onebyte.backend.domain.material.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.onebyte.backend.domain.material.entity.StudyMaterial;

import java.time.LocalDateTime;
import java.util.List;

public record MaterialListResponse(List<Item> materials) {

    public record Item(
            Long materialId,
            String name,
            String fileType,
            long fileSize,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime uploadedAt,
            boolean selected
    ) {
        public static Item from(StudyMaterial m) {
            return new Item(m.getId(), m.getName(), m.getFileType(), m.getFileSize(), m.getUploadedAt(), m.isSelected());
        }
    }

    public static MaterialListResponse from(List<StudyMaterial> materials) {
        return new MaterialListResponse(materials.stream().map(Item::from).toList());
    }
}
