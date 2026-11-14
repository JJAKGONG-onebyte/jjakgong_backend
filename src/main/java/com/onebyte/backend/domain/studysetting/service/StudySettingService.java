package com.onebyte.backend.domain.studysetting.service;

import com.onebyte.backend.domain.studysetting.dto.PomodoroRequest;
import com.onebyte.backend.domain.studysetting.dto.PomodoroResponse;
import com.onebyte.backend.domain.studysetting.dto.ScreenModeRequest;
import com.onebyte.backend.domain.studysetting.dto.ScreenModeResponse;
import com.onebyte.backend.domain.studysetting.dto.StudySettingResponse;
import com.onebyte.backend.domain.studysetting.dto.ToggleRequest;
import com.onebyte.backend.domain.studysetting.dto.TrackingStateResponse;
import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;
import com.onebyte.backend.domain.studysetting.repository.UserStudySettingRepository;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudySettingService {

    private final UserStudySettingRepository userStudySettingRepository;
    private final UserRepository userRepository;

    /** 설정 행이 없는 계정(가입 로직 이전에 만든 계정 등)은 기본값으로 만들어 돌려준다 */
    @Transactional
    public StudySettingResponse getSettings(Long userId) {
        return StudySettingResponse.from(getOrCreate(userId));
    }

    @Transactional
    public PomodoroResponse updatePomodoro(Long userId, PomodoroRequest request) {
        UserStudySetting setting = getOrCreate(userId);
        setting.updatePomodoro(request.studyMinutes(), request.breakMinutes());
        return PomodoroResponse.from(setting);
    }

    @Transactional
    public TrackingStateResponse updateFocusTracking(Long userId, ToggleRequest request) {
        UserStudySetting setting = getOrCreate(userId);
        setting.changeFocusTracking(request.enabled());
        return TrackingStateResponse.from(setting);
    }

    @Transactional
    public TrackingStateResponse updateWebcam(Long userId, ToggleRequest request) {
        UserStudySetting setting = getOrCreate(userId);
        setting.changeWebcam(request.enabled());
        return TrackingStateResponse.from(setting);
    }

    @Transactional
    public ScreenModeResponse updateScreenMode(Long userId, ScreenModeRequest request) {
        UserStudySetting setting = getOrCreate(userId);
        setting.changeScreenMode(request.screenMode());
        return new ScreenModeResponse(setting.getScreenMode());
    }

    private UserStudySetting getOrCreate(Long userId) {
        return userStudySettingRepository.findById(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            return userStudySettingRepository.save(UserStudySetting.createDefault(user));
        });
    }
}
