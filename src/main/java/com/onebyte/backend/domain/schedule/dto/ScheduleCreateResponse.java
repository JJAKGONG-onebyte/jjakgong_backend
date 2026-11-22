package com.onebyte.backend.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 일정 등록 응답 */
public record ScheduleCreateResponse(
        Long scheduleId,
        String title,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime time,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate deadlineDate,
        SchedulePriority priority,
        boolean completed,
        RecurrenceResponse recurrence,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    /** 새로 등록한 일정은 항상 미완료 */
    public static ScheduleCreateResponse of(ScheduleEvent schedule, ScheduleRecurrenceRule rule) {
        return new ScheduleCreateResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getScheduleDate(),
                schedule.getScheduleTime(),
                schedule.getDeadlineDate(),
                schedule.getPriority(),
                false,
                RecurrenceResponse.from(rule),
                schedule.getCreatedAt());
    }
}
