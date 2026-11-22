package com.onebyte.backend.domain.auth.service;

import com.onebyte.backend.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그아웃.
 * 요청에 refresh token이 없어 기기를 특정할 수 없으므로, 비밀번호 변경·재설정과 같이 사용자의 refresh token을 모두 폐기한다.
 * Access Token은 서버에서 즉시 무효화하지 않으며 만료 시간까지 유효하다. (프론트엔드가 저장된 토큰을 삭제)
 */
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** 이미 로그아웃해 폐기할 토큰이 없어도 성공으로 처리한다 */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
