package com.onebyte.backend.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.service.ResolvedOccurrence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 일정 목록 / 캘린더 조회 응답. 반복 일정은 발생일별로 한 항목씩 */
public record ScheduleListResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,
        List<Item> schedules
) {

    public record Item(
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
            @JsonProperty("isRecurring")
            boolean isRecurring
    ) {
        public static Item from(ResolvedOccurrence o) {
            return new Item(o.scheduleId(), o.title(), o.date(), o.occurrenceDate(), o.time(),
                    o.deadlineDate(), o.priority(), o.completed(), o.recurring());
        }
    }

    public static ScheduleListResponse of(LocalDate startDate, LocalDate endDate, List<ResolvedOccurrence> occurrences) {
        return new ScheduleListResponse(startDate, endDate, occurrences.stream().map(Item::from).toList());
    }
}
