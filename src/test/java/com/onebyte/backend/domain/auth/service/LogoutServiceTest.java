package com.onebyte.backend.domain.auth.service;

import com.onebyte.backend.domain.auth.dto.LoginRequest;
import com.onebyte.backend.domain.auth.dto.LoginResponse;
import com.onebyte.backend.domain.auth.dto.RefreshRequest;
import com.onebyte.backend.domain.auth.dto.SignupRequest;
import com.onebyte.backend.domain.auth.repository.RefreshTokenRepository;
import com.onebyte.backend.domain.character.repository.BaseCharacterRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** refresh_token 삭제와 재발급 차단을 실제 DB로 검증한다 (contextLoads와 같은 로컬 MySQL 환경) */
@SpringBootTest
@Transactional
class LogoutServiceTest {

    @Autowired LogoutService logoutService;
    @Autowired AuthService authService;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired BaseCharacterRepository baseCharacterRepository;
    @Autowired EntityManager em;

    private LoginResponse signupAndLogin(String email) {
        Long characterId = baseCharacterRepository.findAll().get(0).getCharacterId();
        authService.signup(new SignupRequest(email, "test1234", "tester", characterId));
        return authService.login(new LoginRequest(email, "test1234"));
    }

    private long tokenCount() {
        em.flush();
        em.clear();
        return refreshTokenRepository.count();
    }

    @Test
    void 로그아웃하면_해당_사용자의_refresh_token을_모두_폐기한다() {
        LoginResponse deviceA = signupAndLogin("logout-a@test.com");
        authService.login(new LoginRequest("logout-a@test.com", "test1234")); // 다른 기기 로그인
        long before = tokenCount();

        logoutService.logout(deviceA.userId());

        assertThat(tokenCount()).isEqualTo(before - 2);
    }

    @Test
    void 로그아웃_후에는_refresh_token으로_재발급할_수_없다() {
        LoginResponse login = signupAndLogin("logout-b@test.com");

        logoutService.logout(login.userId());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(login.refreshToken())))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 이미_로그아웃했어도_다시_호출하면_성공한다() {
        LoginResponse login = signupAndLogin("logout-c@test.com");
        logoutService.logout(login.userId());

        assertThatCode(() -> logoutService.logout(login.userId())).doesNotThrowAnyException();
    }

    @Test
    void 다른_사용자의_토큰에는_영향이_없다() {
        LoginResponse me = signupAndLogin("logout-d@test.com");
        LoginResponse other = signupAndLogin("logout-e@test.com");

        logoutService.logout(me.userId());

        assertThat(authService.refresh(new RefreshRequest(other.refreshToken())).accessToken()).isNotBlank();
    }
}
