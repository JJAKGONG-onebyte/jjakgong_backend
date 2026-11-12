package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysession.entity.StudyBreak;

import java.time.LocalDateTime;

public record BreakStartResponse(
        Long breakId,
        LocalDateTime startedAt,
        int breakMinutes
) {
    public static BreakStartResponse of(StudyBreak studyBreak, int breakMinutes) {
        return new BreakStartResponse(studyBreak.getId(), studyBreak.getStartedAt(), breakMinutes);
    }
}
