package com.onebyte.backend.domain.studysetting.dto;

import com.onebyte.backend.domain.studysetting.entity.FocusSensitivity;
import com.onebyte.backend.domain.studysetting.entity.ScreenMode;
import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;

public record StudySettingResponse(
        int studyMinutes,
        int breakMinutes,
        boolean focusTrackingEnabled,
        boolean webcamEnabled,
        FocusSensitivity focusSensitivity,
        ScreenMode screenMode
) {
    public static StudySettingResponse from(UserStudySetting s) {
        return new StudySettingResponse(
                s.getStudyMinutes(),
                s.getBreakMinutes(),
                s.isFocusTrackingEnabled(),
                s.isWebcamEnabled(),
                s.getFocusSensitivity(),
                s.getScreenMode());
    }
}
