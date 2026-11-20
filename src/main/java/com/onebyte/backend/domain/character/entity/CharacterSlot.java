package com.onebyte.backend.domain.character.entity;

import java.util.Arrays;
import java.util.Optional;

/** 캐릭터 장착 슬롯 (items.slot / user_character_equipment.slot 에 저장되는 값) */
public enum CharacterSlot {
    HAT,
    HAIR,
    TOP,
    BOTTOM,
    ACCESSORY;

    /** 대소문자 구분 없이 변환, 목록에 없으면 empty */
    public static Optional<CharacterSlot> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}