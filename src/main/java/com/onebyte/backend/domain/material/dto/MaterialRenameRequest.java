package com.onebyte.backend.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialRenameRequest(
        @NotBlank(message = "자료 이름을 입력해주세요.")
        @Size(max = 100, message = "자료 이름은 100자 이하여야 합니다.")
        String name
) {
}
