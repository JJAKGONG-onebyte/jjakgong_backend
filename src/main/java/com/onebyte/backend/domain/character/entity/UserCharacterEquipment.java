package com.onebyte.backend.domain.character.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 슬롯별 장착 현황 (슬롯당 아이템 1개) */
@Entity
@Table(name = "user_character_equipment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCharacterEquipment {

    @EmbeddedId
    private UserCharacterEquipmentId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserCharacter userCharacter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    public static UserCharacterEquipment of(UserCharacter userCharacter, String slot, Item item) {
        UserCharacterEquipment equipment = new UserCharacterEquipment();
        equipment.id = new UserCharacterEquipmentId(userCharacter.getUserId(), slot);
        equipment.userCharacter = userCharacter;
        equipment.item = item;
        return equipment;
    }
}
