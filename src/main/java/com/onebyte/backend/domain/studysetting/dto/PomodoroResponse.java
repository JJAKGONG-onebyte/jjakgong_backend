package com.onebyte.backend.domain.studysetting.dto;

import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;

public record PomodoroResponse(int studyMinutes, int breakMinutes) {

    public static PomodoroResponse from(UserStudySetting s) {
        return new PomodoroResponse(s.getStudyMinutes(), s.getBreakMinutes());
    }
}
