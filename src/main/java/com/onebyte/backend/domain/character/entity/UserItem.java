package com.onebyte.backend.domain.character.entity;

import com.onebyte.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 보유 아이템 */
@Entity
@Table(name = "user_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserItem {

    @EmbeddedId
    private UserItemId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("itemId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private LocalDateTime acquiredAt;

    public static UserItem of(User user, Item item) {
        UserItem userItem = new UserItem();
        userItem.id = new UserItemId(user.getId(), item.getId());   // user.getId() → user.getId()
        userItem.user = user;
        userItem.item = item;
        userItem.acquiredAt = LocalDateTime.now();
        return userItem;
    }
}