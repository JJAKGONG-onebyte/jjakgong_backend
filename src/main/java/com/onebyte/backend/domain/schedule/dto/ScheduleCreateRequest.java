package com.onebyte.backend.domain.schedule.dto;

/**
 * 일정 등록 요청.
 * 날짜·시간·우선순위는 형식 오류를 명세서의 에러 코드(INVALID_DATE_FORMAT 등)로 응답하기 위해
 * 문자열로 받고 ScheduleRequestParser에서 변환·검증한다.
 *
 * @param date         yyyy-MM-dd (필수)
 * @param time         HH:mm, null이면 종일 일정
 * @param deadlineDate yyyy-MM-dd, null이면 마감일 없음
 * @param priority     HIGH / MEDIUM / LOW, null이면 MEDIUM
 * @param recurrence   null이면 반복하지 않는 일반 일정
 */
public record ScheduleCreateRequest(
        String title,
        String date,
        String time,
        String deadlineDate,
        String priority,
        RecurrenceRequest recurrence
) {
}
