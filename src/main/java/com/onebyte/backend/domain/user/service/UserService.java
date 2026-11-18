package com.onebyte.backend.domain.user.service;

import com.onebyte.backend.domain.auth.repository.RefreshTokenRepository;
import com.onebyte.backend.domain.user.dto.ChangePasswordRequest;
import com.onebyte.backend.domain.user.dto.ProfileResponse;
import com.onebyte.backend.domain.user.dto.UpdateProfileRequest;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/** 마이페이지: 프로필 조회/수정, 비밀번호 변경 */
@Service
@RequiredArgsConstructor
public class UserService {

    // 가입(SignupRequest)과 같은 규칙
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");
    private static final int NICKNAME_MIN = 2;
    private static final int NICKNAME_MAX = 20;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /** 2-1. 내 프로필 조회 */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return ProfileResponse.from(findUser(userId));
    }

    /** 2-2. 프로필 수정 (닉네임) */
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        String nickname = request.nickname().trim();
        if (nickname.length() < NICKNAME_MIN || nickname.length() > NICKNAME_MAX) {
            throw new CustomException(ErrorCode.INVALID_NICKNAME);
        }
        User user = findUser(userId);
        user.changeNickname(nickname);
        return ProfileResponse.from(user);
    }

    /** 비밀번호 변경. 기존 비밀번호 재확인 후 변경, 모든 기기의 리프레시 토큰 폐기(비밀번호 재설정과 동일) */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (!PASSWORD_PATTERN.matcher(request.newPassword()).matches()) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}