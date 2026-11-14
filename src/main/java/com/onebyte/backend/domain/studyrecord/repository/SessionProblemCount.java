package com.onebyte.backend.domain.studyrecord.repository;

/** 세션별 제출 문제 수와 정답 수 */
public record SessionProblemCount(Long sessionId, Long solvedCount, Long correctCount) {
}
