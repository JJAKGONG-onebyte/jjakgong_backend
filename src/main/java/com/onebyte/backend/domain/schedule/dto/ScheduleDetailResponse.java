package com.onebyte.backend.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import com.onebyte.backend.domain.schedule.service.ResolvedOccurrence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 일정 상세 조회 응답. 반복 일정은 요청한 회차 기준 값 (개별 수정·완료 반영) */
public record ScheduleDetailResponse(
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
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    public static ScheduleDetailResponse of(ResolvedOccurrence o, ScheduleEvent schedule, ScheduleRecurrenceRule rule) {
        return new ScheduleDetailResponse(
                o.scheduleId(),
                o.title(),
                o.date(),
                o.occurrenceDate(),
                o.time(),
                o.deadlineDate(),
                o.priority(),
                o.completed(),
                RecurrenceResponse.from(rule),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt());
    }
}
