package com.onebyte.backend.domain.studysession.dto;

/** materialId가 null이면 자료 선택 해제 (자료 없이 학습) */
public record ChangeMaterialRequest(Long materialId) {
}
