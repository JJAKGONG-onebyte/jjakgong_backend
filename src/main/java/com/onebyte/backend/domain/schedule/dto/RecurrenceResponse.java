package com.onebyte.backend.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;

import java.time.LocalDate;

/** 반복 설정 응답. 반복 일정이 아니면 응답에서 recurrence 자체가 null */
public record RecurrenceResponse(
        RecurrenceFrequency frequency,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,
        Integer count
) {

    public static RecurrenceResponse from(ScheduleRecurrenceRule rule) {
        if (rule == null) {
            return null;
        }
        return new RecurrenceResponse(rule.getFrequency(), rule.getRepeatEndDate(), rule.getRepeatCount());
    }
}
