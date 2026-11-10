package com.onebyte.backend.domain.character.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "base_character")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Builder
    private BaseCharacter(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }
}
