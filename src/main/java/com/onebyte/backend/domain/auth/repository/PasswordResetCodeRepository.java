package com.onebyte.backend.domain.auth.repository;

import com.onebyte.backend.domain.auth.entity.PasswordResetCode;
import com.onebyte.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findFirstByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    /** 새 코드를 발송할 때 이전 코드를 모두 무효화 */
    @Modifying
    @Query("update PasswordResetCode c set c.used = true where c.user = :user and c.used = false")
    int invalidateAllByUser(@Param("user") User user);

    /** 동시 요청에도 값이 유실되지 않도록 DB에서 직접 증가 */
    @Modifying
    @Query("update PasswordResetCode c set c.failedAttempts = c.failedAttempts + 1 where c.resetId = :resetId")
    int increaseFailedAttempts(@Param("resetId") Long resetId);
}
