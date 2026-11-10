package com.onebyte.backend.domain.character.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    /** 장착 슬롯 (값 목록이 정해지면 enum으로 바꿔도 됨) */
    @Column(nullable = false, length = 20)
    private String slot;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Builder
    private Item(String name, String slot, String imageUrl) {
        this.name = name;
        this.slot = slot;
        this.imageUrl = imageUrl;
    }
}
