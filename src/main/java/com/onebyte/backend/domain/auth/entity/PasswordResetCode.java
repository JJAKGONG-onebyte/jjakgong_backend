package com.onebyte.backend.domain.auth.entity;

import com.onebyte.backend.domain.user.entity.User;
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
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "password_reset_code")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reset_id")
    private Long resetId;

    /** 인증코드의 HMAC-SHA256 해시 */
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ColumnDefault("false")
    @Column(name = "used", nullable = false)
    private boolean used;

    /** 틀린 코드 입력 횟수 (ERD에 없던 컬럼 - 추가 필요) */
    @ColumnDefault("0")
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static PasswordResetCode create(User user, String codeHash, LocalDateTime expiresAt) {
        PasswordResetCode code = new PasswordResetCode();
        code.user = user;
        code.codeHash = codeHash;
        code.expiresAt = expiresAt;
        code.used = false;
        code.failedAttempts = 0;
        return code;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void markUsed() {
        this.used = true;
    }
}
