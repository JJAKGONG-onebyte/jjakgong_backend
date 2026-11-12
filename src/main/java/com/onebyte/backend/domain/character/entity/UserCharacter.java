package com.onebyte.backend.domain.character.entity;

import com.onebyte.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** 사용자 캐릭터 (user_id가 PK, created_at이 없어 BaseEntity 대신 updatedAt만 직접 관리) */
@Getter
@Entity
@Table(name = "user_character")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCharacter {

    /** 가입 시점의 기본 색상 (이후 캐릭터 저장 API에서 변경) */
    private static final String DEFAULT_COLOR = "#FFFFFF";
    private static final int NAME_MAX_LENGTH = 20;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    /** HEX (#RRGGBB) */
    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserCharacter create(User user, String name, String color) {
        UserCharacter uc = new UserCharacter();
        uc.user = user;
        uc.name = name;
        uc.color = color;
        return uc;
    }

    /** 가입 시 선택한 기본 캐릭터로 최초 생성: 이름은 기본 캐릭터 이름, 색상은 기본값 */
    public static UserCharacter createInitial(User user, BaseCharacter base) {
        String name = base.getName().length() > NAME_MAX_LENGTH
                ? base.getName().substring(0, NAME_MAX_LENGTH)
                : base.getName();
        return create(user, name, DEFAULT_COLOR);
    }
}
