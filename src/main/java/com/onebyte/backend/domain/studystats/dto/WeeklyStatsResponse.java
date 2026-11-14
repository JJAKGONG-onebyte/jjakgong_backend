package com.onebyte.backend.domain.studystats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record WeeklyStatsResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate weekStartDate,
        int totalMinutes,
        List<Day> days
) {

    /** dayOfWeek: MON ~ SUN */
    public record Day(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            LocalDate date,
            String dayOfWeek,
            int minutes
    ) {
    }
}
