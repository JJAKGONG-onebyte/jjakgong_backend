
package com.onebyte.backend.domain.user.entity;

import com.onebyte.backend.domain.character.entity.BaseCharacter;
import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /** 초기 캐릭터 선택 전에는 NULL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private BaseCharacter character;

    public static User create(String email, String nickname, String encodedPassword) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.password = encodedPassword;
        return user;
    }

    public boolean hasCharacter() {
        return character != null;
    }

    public void selectCharacter(BaseCharacter character) {
        this.character = character;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
