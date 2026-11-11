package com.onebyte.backend.domain.studysession.dto;

import java.time.LocalDateTime;

public record HeartbeatResponse(Long sessionId, LocalDateTime lastActivityAt) {
}
