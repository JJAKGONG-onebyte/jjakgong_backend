package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysession.entity.StudyBreak;

import java.time.LocalDateTime;

public record BreakEndResponse(
        Long breakId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        long breakSeconds,
        long totalBreakSeconds
) {
    public static BreakEndResponse of(StudyBreak studyBreak, long totalBreakSeconds) {
        return new BreakEndResponse(
                studyBreak.getId(),
                studyBreak.getStartedAt(),
                studyBreak.getEndedAt(),
                studyBreak.durationSecondsUntil(studyBreak.getEndedAt()),
                totalBreakSeconds
        );
    }
}
