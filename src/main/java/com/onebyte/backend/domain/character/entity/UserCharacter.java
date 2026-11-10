package com.onebyte.backend.domain.character.entity;

import com.onebyte.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** 사용자별 캐릭터 꾸미기 정보 (users와 1:1, PK = user_id) */
@Entity
@Table(name = "user_character")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCharacter {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 20)
    private String name;

    /** HEX 색상 코드 (예: #2F5BFF) */
    @Column(nullable = false, length = 7)
    private String color;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserCharacter(User user, String name, String color) {
        this.user = user;
        this.name = name;
        this.color = color;
    }
}
