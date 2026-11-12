package com.onebyte.backend.domain.auth.repository;

import com.onebyte.backend.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 해당 해시의 토큰을 즉시 삭제하고 삭제된 행 수를 반환한다.
     * 0이면 이미 폐기/사용된 토큰이다. (벌크 쿼리라 같은 토큰으로 동시에 재발급해도 한 번만 성공)
     */
    @Modifying
    @Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
    int revokeByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from RefreshToken r where r.user.id = :userId")
    int revokeAllByUserId(@Param("userId") Long userId);
}
