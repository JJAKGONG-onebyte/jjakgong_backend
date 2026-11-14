package com.onebyte.backend.domain.studysetting.dto;

import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;

/** 집중 트래킹과 웹캠은 서로 연동되어 한쪽을 바꾸면 다른 쪽도 바뀔 수 있어 둘 다 내려준다 */
public record TrackingStateResponse(boolean focusTrackingEnabled, boolean webcamEnabled) {

    public static TrackingStateResponse from(UserStudySetting s) {
        return new TrackingStateResponse(s.isFocusTrackingEnabled(), s.isWebcamEnabled());
    }
}
