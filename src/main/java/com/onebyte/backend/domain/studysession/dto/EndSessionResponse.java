package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysession.entity.StudySession;

import java.time.LocalDateTime;

public record EndSessionResponse(
        Long sessionId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int recordedMinutes,
        long totalBreakMinutes,
        ProblemStats problemStats,
        FocusEventStats focusEventStats
) {
    public record ProblemStats(long solvedCount, long correctCount) {
    }

    /** away/gazeOff/drowsy 횟수에는 오탐 신고 이벤트를 포함하지 않는다 */
    public record FocusEventStats(long awayCount, long gazeOffCount, long drowsyCount, long falsePositiveCount) {
    }

    public static EndSessionResponse of(StudySession session, long totalBreakMinutes,
                                        ProblemStats problemStats, FocusEventStats focusEventStats) {
        return new EndSessionResponse(
                session.getId(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getRecordedMinutes(),
                totalBreakMinutes,
                problemStats,
                focusEventStats
        );
    }
}
