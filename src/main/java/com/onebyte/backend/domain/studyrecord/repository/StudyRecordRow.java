package com.onebyte.backend.domain.studyrecord.repository;

import java.time.LocalDateTime;

/** 학습 기록 목록 한 줄 (조회 전용) */
public record StudyRecordRow(
        Long sessionId,
        LocalDateTime startedAt,
        Integer recordedMinutes,
        String materialName,
        Long materialId
) {
}
