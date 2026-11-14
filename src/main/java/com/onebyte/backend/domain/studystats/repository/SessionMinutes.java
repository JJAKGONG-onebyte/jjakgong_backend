package com.onebyte.backend.domain.studystats.repository;

import java.time.LocalDateTime;

/** 통계 집계용 조회 결과 (세션 시작 시각, 기록된 학습 시간(분)) */
public record SessionMinutes(LocalDateTime startedAt, Integer recordedMinutes) {
}
