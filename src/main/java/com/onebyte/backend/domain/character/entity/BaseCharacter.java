package com.onebyte.backend.domain.character.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "base_character")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Long characterId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Builder
    private BaseCharacter(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }
}
