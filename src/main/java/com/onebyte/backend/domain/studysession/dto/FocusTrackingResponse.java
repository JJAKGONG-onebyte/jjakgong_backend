package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysession.dto.SessionResponse.FocusStreamResponse;
import com.onebyte.backend.domain.studysession.entity.StudySession;

public record FocusTrackingResponse(
        Long sessionId,
        boolean focusTrackingEnabled,
        FocusStreamResponse focusStream
) {
    public static FocusTrackingResponse from(StudySession session) {
        // focusStream은 AI 서버 연동 전까지 null
        return new FocusTrackingResponse(session.getId(), session.isFocusTrackingEnabled(), null);
    }
}
