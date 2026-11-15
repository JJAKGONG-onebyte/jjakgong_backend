package com.onebyte.backend.domain.material.dto;

/** materialId가 null이면 선택 해제 (자료 없이 학습) */
public record SelectedMaterialRequest(Long materialId) {
}
