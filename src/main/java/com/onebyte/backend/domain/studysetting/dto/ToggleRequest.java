package com.onebyte.backend.domain.studysetting.dto;

import jakarta.validation.constraints.NotNull;

/** 집중 트래킹 / 웹캠 ON·OFF 공용 요청 */
public record ToggleRequest(
        @NotNull(message = "활성화 여부는 필수입니다.")
        Boolean enabled
) {
}
