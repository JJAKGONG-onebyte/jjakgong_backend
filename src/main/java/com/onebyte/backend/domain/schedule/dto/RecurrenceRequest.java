package com.onebyte.backend.domain.schedule.dto;

/**
 * 반복 설정 요청.
 *
 * @param frequency DAILY / WEEKLY / MONTHLY (필수)
 * @param endDate   yyyy-MM-dd, 해당 날짜까지 반복 (count와 동시 지정 불가)
 * @param count     최초 일정을 포함한 반복 횟수 (endDate와 동시 지정 불가)
 */
public record RecurrenceRequest(
        String frequency,
        String endDate,
        Integer count
) {
}
