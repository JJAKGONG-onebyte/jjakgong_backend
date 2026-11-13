package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.studysetting.entity.FocusSensitivity;
import com.onebyte.backend.domain.studysetting.entity.ScreenMode;

import java.time.LocalDateTime;

/** 학습 시작 / 진행 중 세션 조회 응답 */
public record SessionResponse(
        Long sessionId,
        LocalDateTime startedAt,
        boolean pomodoroEnabled,
        int studyMinutes,
        int breakMinutes,
        boolean focusTrackingEnabled,
        FocusSensitivity focusSensitivity,
        ScreenMode screenMode,
        FocusStreamResponse focusStream,
        MaterialResponse material
) {

    public static SessionResponse from(StudySession session) {
        return new SessionResponse(
                session.getId(),
                session.getStartedAt(),
                session.isPomodoroEnabled(),
                session.getStudyMinutes(),
                session.getBreakMinutes(),
                session.isFocusTrackingEnabled(),
                session.getFocusSensitivity(),
                session.getScreenMode(),
                null, // AI 서버 연동 전까지 null
                MaterialResponse.from(session)
        );
    }

    /** 집중 감지 WebSocket 접속 정보 (AI 서버 연동 시 사용) */
    public record FocusStreamResponse(String url, String token, LocalDateTime expiresAt) {
    }

    public record MaterialResponse(Long materialId, String name) {

        static MaterialResponse from(StudySession session) {
            if (session.getMaterialName() == null) {
                return null;
            }
            Long materialId = (session.getMaterial() != null) ? session.getMaterial().getId() : null;
            return new MaterialResponse(materialId, session.getMaterialName());
        }
    }
}
