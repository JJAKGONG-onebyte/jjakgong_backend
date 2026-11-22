package com.onebyte.backend.domain.dashboard.service;

import com.onebyte.backend.domain.auth.dto.SignupRequest;
import com.onebyte.backend.domain.auth.service.AuthService;
import com.onebyte.backend.domain.character.entity.BaseCharacter;
import com.onebyte.backend.domain.character.repository.BaseCharacterRepository;
import com.onebyte.backend.domain.dashboard.dto.DashboardResponse;
import com.onebyte.backend.domain.material.entity.StudyMaterial;
import com.onebyte.backend.domain.material.repository.StudyMaterialRepository;
import com.onebyte.backend.domain.schedule.dto.RecurrenceRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.service.ScheduleService;
import com.onebyte.backend.domain.studysession.entity.SessionEndType;
import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.studysetting.entity.FocusSensitivity;
import com.onebyte.backend.domain.studysetting.entity.ScreenMode;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 여러 도메인 데이터를 조합하므로 로컬 MySQL로 검증한다 (contextLoads와 같은 환경) */
@SpringBootTest
@Transactional
class DashboardServiceTest {

    @Autowired DashboardService dashboardService;
    @Autowired AuthService authService;
    @Autowired ScheduleService scheduleService;
    @Autowired UserRepository userRepository;
    @Autowired BaseCharacterRepository baseCharacterRepository;
    @Autowired StudyMaterialRepository studyMaterialRepository;
    @Autowired EntityManager em;

    private final LocalDate today = LocalDate.now();

    /** 회원가입 흐름으로 만들어 학습 설정·캐릭터가 함께 생성된 사용자 */
    private Long signup(String email, String nickname) {
        Long characterId = baseCharacterRepository.findAll().get(0).getCharacterId();
        return authService.signup(new SignupRequest(email, "test1234", nickname, characterId)).userId();
    }

    private User user(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    /** 학습 종료 API는 다른 이슈 범위라 테스트에서는 세션 시각을 직접 지정한다 */
    private StudySession finishedSession(Long userId, LocalDateTime startedAt, int minutes, String materialName) {
        StudySession session = StudySession.builder()
                .user(user(userId))
                .studyMinutes(50)
                .breakMinutes(10)
                .focusSensitivity(FocusSensitivity.NORMAL)
                .screenMode(ScreenMode.VIDEO_CALL)
                .build();
        ReflectionTestUtils.setField(session, "startedAt", startedAt);
        ReflectionTestUtils.setField(session, "materialName", materialName);
        session.end(startedAt.plusMinutes(minutes), SessionEndType.USER, minutes);
        em.persist(session);
        return session;
    }

    private StudyMaterial material(Long userId, String name, LocalDateTime uploadedAt) {
        StudyMaterial material = StudyMaterial.builder()
                .user(user(userId))
                .name(name)
                .originFileName(name)
                .fileType("PDF")
                .fileSize(1000L)
                .filePath("/tmp/" + name)
                .build();
        ReflectionTestUtils.setField(material, "uploadedAt", uploadedAt);
        em.persist(material);
        return material;
    }

    private DashboardResponse dashboard(Long userId, String date) {
        em.flush();
        em.clear();
        return dashboardService.getDashboard(userId, date);
    }

    @Test
    void 데이터가_없는_신규_사용자() {
        Long userId = signup("dash-new@test.com", "송눈송");

        DashboardResponse response = dashboard(userId, null);

        assertThat(response.date()).isEqualTo(today);
        assertThat(response.user().displayName()).isEqualTo("송눈송");
        assertThat(response.studyTime()).isEqualTo(new DashboardResponse.StudyTime(0, 3000, 0));
        assertThat(response.streak().days()).isZero();
        assertThat(response.character()).isNotNull();
        assertThat(response.character().level()).isEqualTo(1);
        assertThat(response.todaySchedules()).isEmpty();
        assertThat(response.learningMaterials()).isEmpty();
        assertThat(response.recentStudyRecords()).isEmpty();
        assertThat(response.defaultLearningMaterialId()).isNull();
    }

    @Test
    void 캐릭터와_학습_설정이_없으면_캐릭터는_null이고_목표는_설정_기본값() {
        Long userId = userRepository.save(User.create("dash-bare@test.com", "bare", "pw")).getId();

        DashboardResponse response = dashboard(userId, null);

        assertThat(response.character()).isNull();
        assertThat(response.studyTime().goalSeconds()).isEqualTo(3000);
    }

    @Test
    void 오늘_종료된_세션만_학습_시간에_포함하고_달성률을_계산한다() {
        Long userId = signup("dash-time@test.com", "tester");
        finishedSession(userId, today.atTime(9, 0), 20, "운영체제");
        finishedSession(userId, today.atTime(13, 0), 10, null);
        finishedSession(userId, today.minusDays(1).atTime(9, 0), 40, "어제 공부");
        StudySession ongoing = StudySession.builder().user(user(userId)).studyMinutes(50).breakMinutes(10).build();
        em.persist(ongoing);

        DashboardResponse response = dashboard(userId, null);

        assertThat(response.studyTime()).isEqualTo(new DashboardResponse.StudyTime(1800, 3000, 60));
        assertThat(response.streak().days()).isEqualTo(2);
    }

    @Test
    void 달성률은_100을_넘지_않는다() {
        Long userId = signup("dash-cap@test.com", "tester");
        finishedSession(userId, today.atTime(8, 0), 120, null);

        DashboardResponse.StudyTime studyTime = dashboard(userId, null).studyTime();

        assertThat(studyTime.todaySeconds()).isEqualTo(7200);
        assertThat(studyTime.achievementRate()).isEqualTo(100);
    }

    @Test
    void 최근_학습_기록은_종료순_최대_5개() {
        Long userId = signup("dash-records@test.com", "tester");
        for (int i = 1; i <= 6; i++) {
            finishedSession(userId, today.minusDays(i).atTime(10, 0), 30, "자료" + i);
        }

        DashboardResponse response = dashboard(userId, null);

        assertThat(response.recentStudyRecords()).hasSize(DashboardService.RECENT_RECORD_LIMIT);
        assertThat(response.recentStudyRecords().get(0).title()).isEqualTo("자료1");
        assertThat(response.recentStudyRecords().get(0).durationSeconds()).isEqualTo(1800);
        assertThat(response.recentStudyRecords().get(0).endedAt())
                .isEqualTo(today.minusDays(1).atTime(10, 30));
        assertThat(response.recentStudyRecords()).extracting(DashboardResponse.RecentStudyRecord::title)
                .containsExactly("자료1", "자료2", "자료3", "자료4", "자료5");
    }

    @Test
    void 학습_자료는_최근_업로드순_최대_5개이고_선택한_자료가_기본_자료() {
        Long userId = signup("dash-material@test.com", "tester");
        StudyMaterial oldest = material(userId, "자료0.pdf", LocalDateTime.of(2026, 9, 1, 10, 0));
        for (int i = 1; i <= 5; i++) {
            material(userId, "자료" + i + ".pdf", LocalDateTime.of(2026, 9, 1 + i, 10, 0));
        }
        em.flush();
        studyMaterialRepository.selectOnly(userId, oldest.getId());

        DashboardResponse response = dashboard(userId, null);

        assertThat(response.learningMaterials()).extracting(DashboardResponse.LearningMaterial::fileName)
                .containsExactly("자료5.pdf", "자료4.pdf", "자료3.pdf", "자료2.pdf", "자료1.pdf");
        assertThat(response.learningMaterials().get(0).fileType()).isEqualTo("PDF");
        assertThat(response.learningMaterials().get(0).fileSizeBytes()).isEqualTo(1000L);
        assertThat(response.defaultLearningMaterialId()).isEqualTo(oldest.getId());
    }

    @Test
    void 오늘_일정은_일정_도메인의_회차를_재사용한다() {
        Long userId = signup("dash-schedule@test.com", "tester");
        String date = "2026-10-08";
        scheduleService.create(userId, new ScheduleCreateRequest("과제 제출", date, "14:00", null, "HIGH", null));
        scheduleService.create(userId, new ScheduleCreateRequest("영어 복습", "2026-10-01", null, null, null,
                new RecurrenceRequest("DAILY", null, null)));
        scheduleService.create(userId, new ScheduleCreateRequest("다른 날", "2026-10-09", null, null, null, null));

        DashboardResponse response = dashboard(userId, date);

        assertThat(response.date()).isEqualTo(LocalDate.parse(date));
        assertThat(response.todaySchedules())
                .extracting(DashboardResponse.TodaySchedule::title, DashboardResponse.TodaySchedule::time,
                        DashboardResponse.TodaySchedule::occurrenceDate)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("영어 복습", null, LocalDate.parse(date)),
                        org.assertj.core.groups.Tuple.tuple("과제 제출", LocalTime.of(14, 0), LocalDate.parse(date)));
    }

    @Test
    void 다른_사용자의_데이터는_섞이지_않는다() {
        Long me = signup("dash-me@test.com", "me");
        Long other = signup("dash-other@test.com", "other");
        finishedSession(other, today.atTime(9, 0), 30, "남의 공부");
        material(other, "남의자료.pdf", LocalDateTime.now());
        scheduleService.create(other, new ScheduleCreateRequest("남의 일정", today.toString(), null, null, null, null));

        DashboardResponse response = dashboard(me, null);

        assertThat(response.studyTime().todaySeconds()).isZero();
        assertThat(response.recentStudyRecords()).isEmpty();
        assertThat(response.learningMaterials()).isEmpty();
        assertThat(response.todaySchedules()).isEmpty();
    }

    @Test
    void 날짜_형식_오류는_INVALID_DATE_FORMAT() {
        Long userId = signup("dash-date@test.com", "tester");

        assertThatThrownBy(() -> dashboardService.getDashboard(userId, "2026/10/08"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DATE_FORMAT);
    }

    @Test
    void 캐릭터_이미지는_기본_캐릭터_이미지() {
        Long userId = signup("dash-char@test.com", "tester");
        BaseCharacter base = baseCharacterRepository.findAll().get(0);

        DashboardResponse.CharacterInfo character = dashboard(userId, null).character();

        assertThat(character.characterId()).isEqualTo(base.getCharacterId());
        assertThat(character.appearanceUrl()).isEqualTo(base.getImageUrl());
    }
}
