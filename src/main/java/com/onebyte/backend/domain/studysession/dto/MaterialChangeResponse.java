package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysession.dto.SessionResponse.MaterialResponse;
import com.onebyte.backend.domain.studysession.entity.StudySession;

public record MaterialChangeResponse(
        Long sessionId,
        MaterialResponse material
) {
    public static MaterialChangeResponse from(StudySession session) {
        return new MaterialChangeResponse(session.getId(), MaterialResponse.from(session));
    }
}
