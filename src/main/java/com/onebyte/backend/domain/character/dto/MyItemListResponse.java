package com.onebyte.backend.domain.character.dto;

import com.onebyte.backend.domain.character.entity.UserItem;

import java.time.LocalDateTime;
import java.util.List;

/** 3-3. 보유 아이템 목록 응답 */
public record MyItemListResponse(List<Item> items) {

    public record Item(Long itemId, String name, String slot, String imageUrl, LocalDateTime acquiredAt) {
    }

    public static MyItemListResponse from(List<UserItem> userItems) {
        return new MyItemListResponse(userItems.stream()
                .map(ui -> new Item(
                        ui.getItem().getId(),
                        ui.getItem().getName(),
                        ui.getItem().getSlot(),
                        ui.getItem().getImageUrl(),
                        ui.getAcquiredAt()))
                .toList());
    }
}