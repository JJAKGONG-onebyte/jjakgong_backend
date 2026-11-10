package com.onebyte.backend.domain.character.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** user_item 복합키 (user_id, item_id) */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserItemId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "item_id")
    private Long itemId;
}
