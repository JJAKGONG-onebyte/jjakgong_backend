package com.onebyte.backend.domain.schedule.dto;

/**
 * 일정 완료 상태 변경 요청 (토글이 아닌 명시적 설정).
 *
 * @param completed true: 완료, false: 완료 취소 (필수)
 */
public record ScheduleCompletionRequest(Boolean completed) {
}
