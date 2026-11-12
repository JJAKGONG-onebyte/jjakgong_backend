package com.onebyte.backend.domain.character.dto;

import com.onebyte.backend.domain.character.entity.BaseCharacter;

import java.util.List;

public record BaseCharacterListResponse(List<Item> characters) {

    public record Item(Long characterId, String name, String imageUrl) {
    }

    public static BaseCharacterListResponse from(List<BaseCharacter> characters) {
        return new BaseCharacterListResponse(characters.stream()
                .map(c -> new Item(c.getCharacterId(), c.getName(), c.getImageUrl()))
                .toList());
    }
}
