package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 검증을 마친 일정 등록 값.
 *
 * @param recurrence null이면 반복하지 않는 일반 일정
 */
public record ScheduleCreateCommand(
        String title,
        LocalDate date,
        LocalTime time,
        LocalDate deadlineDate,
        SchedulePriority priority,
        Recurrence recurrence
) {

    /** endDate·count 둘 다 null이면 종료 조건 없는 반복 */
    public record Recurrence(RecurrenceFrequency frequency, LocalDate endDate, Integer count) {
    }
}
