package com.onebyte.backend.domain.studysetting.dto;

import com.onebyte.backend.domain.studysetting.entity.ScreenMode;
import jakarta.validation.constraints.NotNull;

public record ScreenModeRequest(
        @NotNull(message = "학습 화면 모드는 필수입니다.")
        ScreenMode screenMode
) {
}
