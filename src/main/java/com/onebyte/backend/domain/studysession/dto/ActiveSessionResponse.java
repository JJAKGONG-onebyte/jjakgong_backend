package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysession.entity.StudyBreak;
import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.studysetting.entity.FocusSensitivity;
import com.onebyte.backend.domain.studysetting.entity.ScreenMode;

import java.time.LocalDateTime;

/** 진행 중 세션 조회 응답 (학습 시작 응답 + 휴식 정보) */
public record ActiveSessionResponse(
        Long sessionId,
        LocalDateTime startedAt,
        boolean pomodoroEnabled,
        int studyMinutes,
        int breakMinutes,
        boolean focusTrackingEnabled,
        FocusSensitivity focusSensitivity,
        ScreenMode screenMode,
        long totalBreakSeconds,
        CurrentBreakResponse currentBreak,
        SessionResponse.FocusStreamResponse focusStream,
        SessionResponse.MaterialResponse material
) {

    /**
     * @param totalBreakSeconds 끝난 휴식 시간 합계(초)
     * @param currentBreak      진행 중인 휴식 (없으면 null)
     */
    public static ActiveSessionResponse of(StudySession session, long totalBreakSeconds, StudyBreak currentBreak) {
        SessionResponse base = SessionResponse.from(session);
        return new ActiveSessionResponse(
                base.sessionId(),
                base.startedAt(),
                base.pomodoroEnabled(),
                base.studyMinutes(),
                base.breakMinutes(),
                base.focusTrackingEnabled(),
                base.focusSensitivity(),
                base.screenMode(),
                totalBreakSeconds,
                (currentBreak != null) ? new CurrentBreakResponse(currentBreak.getId(), currentBreak.getStartedAt()) : null,
                base.focusStream(),
                base.material()
        );
    }

    public record CurrentBreakResponse(Long breakId, LocalDateTime startedAt) {
    }
}
