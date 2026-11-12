package com.onebyte.backend.domain.auth.service;

import com.onebyte.backend.domain.auth.dto.PasswordResetConfirmRequest;
import com.onebyte.backend.domain.auth.dto.PasswordResetSendRequest;
import com.onebyte.backend.domain.auth.dto.PasswordResetSendResponse;
import com.onebyte.backend.domain.auth.entity.PasswordResetCode;
import com.onebyte.backend.domain.auth.event.PasswordResetMailEvent;
import com.onebyte.backend.domain.auth.repository.PasswordResetCodeRepository;
import com.onebyte.backend.domain.auth.repository.RefreshTokenRepository;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import com.onebyte.backend.global.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int CODE_VALID_SECONDS = 600;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${password-reset.hmac-secret}")
    private String hmacSecret;

    /** 가입 여부 노출 방지: 미가입 이메일이어도 동일하게 성공 응답 (메일은 미발송) */
    @Transactional
    public PasswordResetSendResponse send(PasswordResetSendRequest request) {
        userRepository.findByEmail(normalizeEmail(request.email())).ifPresent(user -> {
            passwordResetCodeRepository.invalidateAllByUser(user);

            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            passwordResetCodeRepository.save(PasswordResetCode.create(
                    user,
                    hash(user, code),
                    LocalDateTime.now().plusSeconds(CODE_VALID_SECONDS)));

            // 트랜잭션 커밋 후 비동기로 메일 발송
            eventPublisher.publishEvent(new PasswordResetMailEvent(user.getEmail(), code));
        });
        return new PasswordResetSendResponse(CODE_VALID_SECONDS);
    }

    /**
     * 코드가 틀렸을 때 failed_attempts 증가분이 롤백되지 않도록 CustomException에서는 롤백하지 않는다.
     */
    @Transactional(noRollbackFor = CustomException.class)
    public void confirm(PasswordResetConfirmRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CODE));

        PasswordResetCode reset = passwordResetCodeRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CODE));

        if (reset.isExpired(LocalDateTime.now()) || reset.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new CustomException(ErrorCode.INVALID_CODE);
        }

        if (!HashUtil.constantTimeEquals(reset.getCodeHash(), hash(user, request.code()))) {
            passwordResetCodeRepository.increaseFailedAttempts(reset.getResetId());
            throw new CustomException(ErrorCode.INVALID_CODE);
        }

        reset.markUsed();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // 비밀번호가 바뀌면 기존에 로그인된 모든 기기의 리프레시 토큰을 폐기
        refreshTokenRepository.revokeAllByUserId(user.getUserId());
    }

    private String hash(User user, String code) {
        return HashUtil.hmacSha256Hex(hmacSecret, user.getUserId() + ":" + code);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
