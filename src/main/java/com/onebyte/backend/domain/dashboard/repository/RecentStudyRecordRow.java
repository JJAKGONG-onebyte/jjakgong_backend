package com.onebyte.backend.domain.dashboard.repository;

import java.time.LocalDateTime;

/** 대시보드 최근 학습 기록 한 줄 (조회 전용) */
public record RecentStudyRecordRow(
        Long sessionId,
        String materialName,
        Integer recordedMinutes,
        LocalDateTime endedAt
) {
}
