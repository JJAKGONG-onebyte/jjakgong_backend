package com.onebyte.backend.domain.character.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** user_character_equipment 복합키 (user_id, slot) */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserCharacterEquipmentId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "slot", length = 20)
    private String slot;
}
