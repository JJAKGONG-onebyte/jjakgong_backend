package com.onebyte.backend.domain.auth.service;

import com.onebyte.backend.domain.auth.dto.LoginRequest;
import com.onebyte.backend.domain.auth.dto.LoginResponse;
import com.onebyte.backend.domain.auth.dto.RefreshRequest;
import com.onebyte.backend.domain.auth.dto.SignupRequest;
import com.onebyte.backend.domain.auth.dto.SignupResponse;
import com.onebyte.backend.domain.auth.dto.TokenResponse;
import com.onebyte.backend.domain.auth.entity.RefreshToken;
import com.onebyte.backend.domain.auth.repository.RefreshTokenRepository;
import com.onebyte.backend.domain.character.entity.BaseCharacter;
import com.onebyte.backend.domain.character.entity.UserCharacter;
import com.onebyte.backend.domain.character.repository.BaseCharacterRepository;
import com.onebyte.backend.domain.character.repository.UserCharacterRepository;
import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;
import com.onebyte.backend.domain.studysetting.repository.UserStudySettingRepository;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import com.onebyte.backend.global.jwt.JwtProvider;
import com.onebyte.backend.global.util.HashUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserStudySettingRepository userStudySettingRepository;
    private final BaseCharacterRepository baseCharacterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /** 존재하지 않는 이메일로 로그인할 때도 BCrypt 비교를 해서 응답 시간 차이로 가입 여부가 드러나지 않게 함 */
    private String dummyHash;

    @PostConstruct
    void initDummyHash() {
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /** 가입 정보 + 선택한 기본 캐릭터를 한 트랜잭션에서 처리 (캐릭터 없는 반쯤 가입된 계정이 생기지 않음) */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }
        BaseCharacter character = baseCharacterRepository.findById(request.characterId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        User user = User.create(email, request.nickname().trim(), passwordEncoder.encode(request.password()));
        user.selectCharacter(character);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // existsByEmail 이후 동시에 같은 이메일로 가입한 경우 (users.email unique)
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }

        userStudySettingRepository.save(UserStudySetting.createDefault(user));
        userCharacterRepository.save(UserCharacter.createInitial(user, character));
        return SignupResponse.from(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);

        String hashToCompare = user != null ? user.getPassword() : dummyHash;
        boolean matches = passwordEncoder.matches(request.password(), hashToCompare);
        if (user == null || !matches) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        TokenResponse tokens = issueTokens(user);
        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                user.getId(),
                user.getNickname());
    }

    /** 리프레시 토큰 rotation: 기존 토큰은 폐기하고 새 access/refresh를 발급 */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtProvider.validate(refreshToken, JwtProvider.REFRESH)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 삭제된 행이 0이면 이미 사용/폐기된 토큰 (동시에 두 번 요청해도 한 번만 성공)
        int revoked = refreshTokenRepository.revokeByTokenHash(HashUtil.sha256Hex(refreshToken));
        if (revoked == 0) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(jwtProvider.getUserId(refreshToken))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.create(
                user,
                HashUtil.sha256Hex(refreshToken),
                LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L)));
        return new TokenResponse(accessToken, refreshToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
