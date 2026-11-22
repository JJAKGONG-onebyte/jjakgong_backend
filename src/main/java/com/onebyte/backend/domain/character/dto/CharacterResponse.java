package com.onebyte.backend.domain.character.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 3-1. 내 캐릭터 조회 응답 */
public record CharacterResponse(
        Long characterId,
        String characterImageUrl,
        String name,
        int level,
        int minutesToNextLevel,
        int levelProgressPercent,
        String color,
        List<Customization> customizations,
        LocalDateTime updatedAt
) {

    /** 슬롯별 장착 현황 (미장착이면 itemId, imageUrl이 null) */
    public record Customization(String slot, Long itemId, String imageUrl) {
    }
}