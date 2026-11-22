package com.onebyte.backend.domain.focus.dto;

import com.onebyte.backend.domain.focus.entity.FocusEvent;
import com.onebyte.backend.domain.focus.entity.FocusEventType;

public record FocusEventResponse(
        Long eventId,
        FocusEventType eventType,
        int durationSeconds,
        boolean falsePositive
) {
    public static FocusEventResponse from(FocusEvent event) {
        return new FocusEventResponse(
                event.getId(), event.getEventType(), event.getDurationSeconds(), event.isFalsePositive());
    }
}
