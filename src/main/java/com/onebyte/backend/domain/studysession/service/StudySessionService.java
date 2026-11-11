package com.onebyte.backend.domain.studysession.service;

import com.onebyte.backend.domain.material.entity.StudyMaterial;
import com.onebyte.backend.domain.studysession.dto.ActiveSessionResponse;
import com.onebyte.backend.domain.studysession.dto.HeartbeatResponse;
import com.onebyte.backend.domain.studysession.dto.SessionResponse;
import com.onebyte.backend.domain.studysession.dto.StartSessionRequest;
import com.onebyte.backend.domain.studysession.entity.StudyBreak;
import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.studysession.repository.StudyBreakRepository;
import com.onebyte.backend.domain.studysession.repository.StudySessionRepository;
import com.onebyte.backend.domain.studysetting.entity.ScreenMode;
import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final StudyBreakRepository studyBreakRepository;
    private final StudySessionCloser studySessionCloser;
    private final StudySessionFinder studySessionFinder;
    // User, UserStudySetting, StudyMaterial은 다른 담당 도메인이라 Repository가 생기기 전까지 EntityManager로 조회
    private final EntityManager em;

    /** 학습 시작 */
    @Transactional
    public SessionResponse start(Long userId, StartSessionRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 진행 중 세션 확인: 최근 활동이 있으면 409, 10분 넘게 활동이 없으면 자동 종료
        Optional<StudySession> ongoing = studySessionRepository
                .findFirstByUser_IdAndEndedAtIsNullOrderByStartedAtDesc(userId);
        if (ongoing.isPresent()) {
            StudySession active = ongoing.get();
            if (!studySessionCloser.isInactive(active, now)) {
                throw new CustomException(ErrorCode.ACTIVE_SESSION_EXISTS);
            }
            studySessionCloser.closeAsInactive(active);
        }

        User user = em.getReference(User.class, userId);
        UserStudySetting setting = getOrCreateSetting(user, userId);

        // 2. 집중 트래킹: 요청값이 없으면 설정의 트래킹 && 웹캠
        boolean focusTrackingEnabled;
        if (request.focusTrackingEnabled() == null) {
            focusTrackingEnabled = setting.isFocusTrackingEnabled() && setting.isWebcamEnabled();
        } else if (request.focusTrackingEnabled() && !setting.isWebcamEnabled()) {
            throw new CustomException(ErrorCode.WEBCAM_DISABLED);
        } else {
            focusTrackingEnabled = request.focusTrackingEnabled();
        }

        // 3. 학습 화면 모드: 요청값이 있으면 다음 학습 기본값으로 저장
        ScreenMode screenMode = setting.getScreenMode();
        if (request.screenMode() != null) {
            screenMode = request.screenMode();
            setting.changeScreenMode(screenMode);
        }

        StudySession session = StudySession.builder()
                .user(user)
                .material(findOwnedMaterial(request.materialId(), userId))
                .studyMinutes(setting.getStudyMinutes())
                .breakMinutes(setting.getBreakMinutes())
                .pomodoroEnabled(request.pomodoroEnabled() == null || request.pomodoroEnabled())
                .focusTrackingEnabled(focusTrackingEnabled)
                .focusSensitivity(setting.getFocusSensitivity())
                .screenMode(screenMode)
                .build();

        return SessionResponse.from(studySessionRepository.save(session));
    }

    /**
     * 진행 중 세션 조회 (새로고침·재접속 시 복구용).
     * 10분 넘게 활동이 없던 세션은 마지막 활동 시각 기준으로 종료하고 404를 던진다.
     */
    @Transactional(noRollbackFor = CustomException.class)
    public ActiveSessionResponse getActive(Long userId) {
        StudySession session = studySessionRepository
                .findFirstByUser_IdAndEndedAtIsNullOrderByStartedAtDesc(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_ACTIVE_SESSION));

        if (studySessionCloser.isInactive(session, LocalDateTime.now())) {
            studySessionCloser.closeAsInactive(session);
            throw new CustomException(ErrorCode.NO_ACTIVE_SESSION);
        }

        long totalBreakSeconds = 0;
        StudyBreak currentBreak = null;
        List<StudyBreak> breaks = studyBreakRepository.findAllBySession_Id(session.getId());
        for (StudyBreak studyBreak : breaks) {
            if (studyBreak.isOngoing()) {
                currentBreak = studyBreak;
            } else {
                totalBreakSeconds += studyBreak.durationSecondsUntil(studyBreak.getEndedAt());
            }
        }
        return ActiveSessionResponse.of(session, totalBreakSeconds, currentBreak);
    }

    /** 세션 활동 유지 (학습 화면이 1분마다 호출) */
    @Transactional(noRollbackFor = CustomException.class)
    public HeartbeatResponse heartbeat(Long sessionId, Long userId) {
        StudySession session = studySessionFinder.getActiveSession(sessionId, userId);
        return new HeartbeatResponse(session.getId(), session.getLastActivityAt());
    }

    private UserStudySetting getOrCreateSetting(User user, Long userId) {
        UserStudySetting setting = em.find(UserStudySetting.class, userId);
        if (setting == null) {
            setting = UserStudySetting.createDefault(user);
            em.persist(setting);
        }
        return setting;
    }

    private StudyMaterial findOwnedMaterial(Long materialId, Long userId) {
        if (materialId == null) {
            return null;
        }
        StudyMaterial material = em.find(StudyMaterial.class, materialId);
        if (material == null || !material.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.MATERIAL_NOT_FOUND);
        }
        return material;
    }
}
