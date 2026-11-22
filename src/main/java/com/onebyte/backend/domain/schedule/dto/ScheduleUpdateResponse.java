package com.onebyte.backend.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import com.onebyte.backend.domain.schedule.service.ResolvedOccurrence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 일정 수정 응답 (수정된 회차 기준).
 * 반복 일정을 이후 전체 수정해 시리즈가 나뉘면 scheduleId는 새 시리즈의 ID다.
 */
public record ScheduleUpdateResponse(
        Long scheduleId,
        String title,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate occurrenceDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime time,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate deadlineDate,
        SchedulePriority priority,
        boolean completed,
        RecurrenceResponse recurrence,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    public static ScheduleUpdateResponse of(ResolvedOccurrence o, ScheduleRecurrenceRule rule, LocalDateTime updatedAt) {
        return new ScheduleUpdateResponse(
                o.scheduleId(),
                o.title(),
                o.date(),
                o.occurrenceDate(),
                o.time(),
                o.deadlineDate(),
                o.priority(),
                o.completed(),
                RecurrenceResponse.from(rule),
                updatedAt);
    }
}
