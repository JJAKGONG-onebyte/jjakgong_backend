package com.onebyte.backend.domain.dashboard.service;

import com.onebyte.backend.domain.character.repository.UserCharacterRepository;
import com.onebyte.backend.domain.character.service.CharacterService;
import com.onebyte.backend.domain.dashboard.dto.DashboardResponse;
import com.onebyte.backend.domain.dashboard.repository.DashboardQueryRepository;
import com.onebyte.backend.domain.schedule.service.ScheduleRequestParser;
import com.onebyte.backend.domain.schedule.service.ScheduleService;
import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;
import com.onebyte.backend.domain.studysetting.repository.UserStudySettingRepository;
import com.onebyte.backend.domain.studystats.service.StudyStatsService;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 홈/대시보드 통합 조회. 각 도메인의 기존 서비스·데이터를 조합하며 값을 임의로 만들지 않는다.
 *
 * - 학습 시간: 기준 날짜에 시작해 종료된 세션의 기록 시간 (진행 중 세션 제외, 학습 통계와 같은 기준)
 * - 목표 시간: 학습 설정의 세션당 학습 시간(뽀모도로)
 * - 연속 학습일: StudyStatsService (오늘 기준)
 * - 캐릭터: CharacterService (캐릭터 조회 API와 같은 레벨 계산)
 * - 오늘 일정: ScheduleService (반복 일정 회차·완료 상태 반영)
 * 날짜 기준은 서버 기본 시간대(KST)이다.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    static final int MATERIAL_LIMIT = 5;
    static final int RECENT_RECORD_LIMIT = 5;
    private static final int MAX_ACHIEVEMENT_RATE = 100;

    private final UserRepository userRepository;
    private final UserStudySettingRepository studySettingRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterService characterService;
    private final StudyStatsService studyStatsService;
    private final ScheduleService scheduleService;
    private final ScheduleRequestParser requestParser;
    private final DashboardQueryRepository dashboardQueryRepository;

    /** date를 생략하면 오늘 기준 */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, String dateRaw) {
        LocalDate date = Optional.ofNullable(requestParser.parseOptionalDate(dateRaw)).orElseGet(LocalDate::now);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new DashboardResponse(
                date,
                new DashboardResponse.UserInfo(user.getNickname()),
                studyTime(user, date),
                new DashboardResponse.Streak(studyStatsService.getStreak(userId).streakDays()),
                character(user),
                scheduleService.findOccurrences(userId, date, date).stream()
                        .map(DashboardResponse.TodaySchedule::from)
                        .toList(),
                dashboardQueryRepository.findRecentMaterials(userId, PageRequest.of(0, MATERIAL_LIMIT)).stream()
                        .map(DashboardResponse.LearningMaterial::from)
                        .toList(),
                dashboardQueryRepository.findRecentFinished(userId, PageRequest.of(0, RECENT_RECORD_LIMIT)).stream()
                        .map(DashboardResponse.RecentStudyRecord::from)
                        .toList(),
                defaultMaterialId(userId));
    }

    private DashboardResponse.StudyTime studyTime(User user, LocalDate date) {
        long todayMinutes = dashboardQueryRepository.sumFinishedMinutes(
                user.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        long todaySeconds = todayMinutes * 60;
        long goalSeconds = goalMinutes(user) * 60L;

        int achievementRate = (goalSeconds <= 0)
                ? 0
                : (int) Math.min(MAX_ACHIEVEMENT_RATE, todaySeconds * 100 / goalSeconds);
        return new DashboardResponse.StudyTime(todaySeconds, goalSeconds, achievementRate);
    }

    /** 학습 설정이 아직 없으면 학습 설정 도메인의 기본값을 사용한다 (조회 API라 저장하지 않음) */
    private int goalMinutes(User user) {
        return studySettingRepository.findById(user.getId())
                .orElseGet(() -> UserStudySetting.createDefault(user))
                .getStudyMinutes();
    }

    /** 사용자 캐릭터가 아직 없으면 null */
    private DashboardResponse.CharacterInfo character(User user) {
        if (!user.hasCharacter() || !userCharacterRepository.existsById(user.getId())) {
            return null;
        }
        return DashboardResponse.CharacterInfo.from(characterService.getMyCharacter(user.getId()));
    }

    private Long defaultMaterialId(Long userId) {
        List<Long> selected = dashboardQueryRepository.findSelectedMaterialIds(userId);
        return selected.isEmpty() ? null : selected.get(0);
    }
}
