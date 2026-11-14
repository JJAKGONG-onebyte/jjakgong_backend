package com.onebyte.backend.domain.studysetting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PomodoroRequest(
        @NotNull(message = "학습 시간을 입력해주세요.")
        @Min(value = 1, message = "학습 시간은 1~180분 사이여야 합니다.")
        @Max(value = 180, message = "학습 시간은 1~180분 사이여야 합니다.")
        Integer studyMinutes,

        @NotNull(message = "휴식 시간을 입력해주세요.")
        @Min(value = 1, message = "휴식 시간은 1~60분 사이여야 합니다.")
        @Max(value = 60, message = "휴식 시간은 1~60분 사이여야 합니다.")
        Integer breakMinutes
) {
}
