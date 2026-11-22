package com.onebyte.backend.domain.character.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 3-2. 캐릭터 저장(저장하기) 요청: 색상 + 슬롯별 장착 (전체 교체) */
public record SaveCharacterRequest(
        @NotBlank(message = "색상을 입력해주세요.")
        String color,

        @NotNull(message = "customizations는 필수입니다.")
        @Valid
        List<Customization> customizations
) {

    /** itemId가 null이면 해당 슬롯 해제 */
    public record Customization(
            @NotBlank(message = "슬롯을 입력해주세요.")
            String slot,
            Long itemId
    ) {
    }
}