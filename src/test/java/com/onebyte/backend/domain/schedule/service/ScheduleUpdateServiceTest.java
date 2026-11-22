package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.RecurrenceRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleUpdateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleUpdateResponse;
import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrenceId;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import com.onebyte.backend.domain.schedule.repository.ScheduleEventRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleOccurrenceRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleRecurrenceRuleRepository;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 일정 수정은 회차 상태 이전·시리즈 분할 등 DB 반영이 핵심이라 로컬 MySQL로 검증한다 (contextLoads와 같은 환경) */
@SpringBootTest
@Transactional
class ScheduleUpdateServiceTest {

    @Autowired ScheduleUpdateService updateService;
    @Autowired ScheduleService scheduleService;
    @Autowired ScheduleEventRepository scheduleEventRepository;
    @Autowired ScheduleRecurrenceRuleRepository ruleRepository;
    @Autowired ScheduleOccurrenceRepository occurrenceRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager em;
    @Autowired ObjectMapper objectMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(User.create("update-test@test.com", "tester", "pw")).getId();
    }

    private static LocalDate d(String date) {
        return LocalDate.parse(date);
    }

    private ScheduleUpdateRequest body(String json) {
        return objectMapper.readValue(json, ScheduleUpdateRequest.class);
    }

    private Long createNormal(String date) {
        return scheduleService.create(userId,
                new ScheduleCreateRequest("과제", date, "14:00", null, "HIGH", null)).scheduleId();
    }

    private Long createDaily(String date, String endDate, Integer count) {
        return scheduleService.create(userId, new ScheduleCreateRequest(
                "복습", date, "20:00", null, "LOW", new RecurrenceRequest("DAILY", endDate, count))).scheduleId();
    }

    /** 완료 처리 API는 Issue 5 범위라 테스트에서는 회차 상태를 직접 저장한다 */
    private void saveState(Long scheduleId, String date, Map<String, Object> fields) {
        ScheduleOccurrence state = ScheduleOccurrence.of(scheduleEventRepository.findById(scheduleId).orElseThrow(), d(date));
        fields.forEach((name, value) -> ReflectionTestUtils.setField(state, name, value));
        occurrenceRepository.save(state);
        em.flush();
    }

    private List<ResolvedOccurrence> list(String from, String to) {
        em.flush();
        em.clear();
        return scheduleService.findOccurrences(userId, d(from), d(to));
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(code);
    }

    // ===== 요청 본문 =====

    @Test
    void 필드_누락과_null을_구분한다() {
        ScheduleUpdateRequest request = body("{\"time\": null, \"priority\": \"LOW\"}");

        assertThat(request.hasTime()).isTrue();
        assertThat(request.time()).isNull();
        assertThat(request.hasPriority()).isTrue();
        assertThat(request.hasTitle()).isFalse();
        assertThat(request.hasDeadlineDate()).isFalse();
        assertThat(request.hasRecurrence()).isFalse();
    }

    @Test
    void 수정할_항목이_없으면_400() {
        Long id = createNormal("2026-10-08");
        assertError(() -> updateService.update(userId, id, null, null, body("{}")), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 없는_일정이나_남의_일정은_404() {
        Long id = createNormal("2026-10-08");
        Long otherUserId = userRepository.save(User.create("other@test.com", "other", "pw")).getId();

        assertError(() -> updateService.update(otherUserId, id, null, null, body("{\"title\":\"x\"}")),
                ErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> updateService.update(userId, -1L, null, null, body("{\"title\":\"x\"}")),
                ErrorCode.SCHEDULE_NOT_FOUND);
    }

    // ===== 일반 일정 =====

    @Test
    void 일반_일정_부분_수정은_보낸_필드만_바꾼다() {
        Long id = scheduleService.create(userId, new ScheduleCreateRequest(
                "과제", "2026-10-08", "14:00", "2026-10-10", "HIGH", null)).scheduleId();

        ScheduleUpdateResponse response = updateService.update(userId, id, null, null,
                body("{\"title\": \"과제 최종 제출\", \"time\": null}"));

        assertThat(response.title()).isEqualTo("과제 최종 제출");
        assertThat(response.time()).isNull();
        assertThat(response.deadlineDate()).isEqualTo(d("2026-10-10"));
        assertThat(response.priority()).isEqualTo(SchedulePriority.HIGH);
        assertThat(response.recurrence()).isNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void 일반_일정에_수정_범위를_지정하면_400() {
        Long id = createNormal("2026-10-08");
        assertError(() -> updateService.update(userId, id, "2026-10-08", "THIS", body("{\"title\":\"x\"}")),
                ErrorCode.INVALID_UPDATE_SCOPE);
    }

    @Test
    void 일반_일정_검증은_등록과_같다() {
        Long id = createNormal("2026-10-08");
        assertError(() -> updateService.update(userId, id, null, null, body("{\"title\":\" \"}")),
                ErrorCode.TITLE_REQUIRED);
        assertError(() -> updateService.update(userId, id, null, null, body("{\"date\":\"2026/10/09\"}")),
                ErrorCode.INVALID_DATE_FORMAT);
        assertError(() -> updateService.update(userId, id, null, null, body("{\"priority\":null}")),
                ErrorCode.INVALID_INPUT);
        assertError(() -> updateService.update(userId, id, null, null, body("{\"deadlineDate\":\"2026-10-01\"}")),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 일반_일정_날짜를_바꾸면_완료_상태도_옮긴다() {
        Long id = createNormal("2026-10-08");
        saveState(id, "2026-10-08", Map.of("isCompleted", true));

        updateService.update(userId, id, null, null, body("{\"date\": \"2026-10-12\"}"));

        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::date, ResolvedOccurrence::completed)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(d("2026-10-12"), true));
        assertThat(occurrenceRepository.findById(new ScheduleOccurrenceId(id, d("2026-10-08")))).isEmpty();
    }

    @Test
    void 일반_일정에_반복_설정을_지정하면_반복_일정으로_전환() {
        Long id = createNormal("2026-10-08");

        ScheduleUpdateResponse response = updateService.update(userId, id, null, null,
                body("{\"recurrence\": {\"frequency\": \"DAILY\", \"endDate\": \"2026-10-10\"}}"));

        assertThat(response.recurrence().frequency()).isEqualTo(RecurrenceFrequency.DAILY);
        assertThat(list("2026-10-01", "2026-10-31")).extracting(ResolvedOccurrence::occurrenceDate)
                .containsExactly(d("2026-10-08"), d("2026-10-09"), d("2026-10-10"));
    }

    // ===== 반복 일정 공통 =====

    @Test
    void 반복_일정은_scope와_occurrenceDate가_필요하다() {
        Long id = createDaily("2026-10-08", "2026-10-12", null);
        assertError(() -> updateService.update(userId, id, "2026-10-09", null, body("{\"title\":\"x\"}")),
                ErrorCode.INVALID_UPDATE_SCOPE);
        assertError(() -> updateService.update(userId, id, null, "THIS", body("{\"title\":\"x\"}")),
                ErrorCode.INVALID_UPDATE_SCOPE);
        assertError(() -> updateService.update(userId, id, "2026-10-09", "ALL", body("{\"title\":\"x\"}")),
                ErrorCode.INVALID_UPDATE_SCOPE);
    }

    @Test
    void 발생일이_아니거나_삭제된_회차는_404() {
        Long id = createDaily("2026-10-08", "2026-10-12", null);
        saveState(id, "2026-10-10", Map.of("isDeleted", true));

        assertError(() -> updateService.update(userId, id, "2026-10-20", "THIS", body("{\"title\":\"x\"}")),
                ErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> updateService.update(userId, id, "2026-10-10", "THIS", body("{\"title\":\"x\"}")),
                ErrorCode.SCHEDULE_NOT_FOUND);
    }

    // ===== THIS =====

    @Test
    void THIS는_선택한_회차만_바꾼다() {
        Long id = createDaily("2026-10-08", "2026-10-10", null);

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-09", "THIS",
                body("{\"title\": \"시험 대비\", \"time\": null, \"priority\": \"HIGH\", \"deadlineDate\": \"2026-10-11\"}"));

        assertThat(response.occurrenceDate()).isEqualTo(d("2026-10-09"));
        assertThat(response.title()).isEqualTo("시험 대비");
        assertThat(response.time()).isNull();
        assertThat(response.priority()).isEqualTo(SchedulePriority.HIGH);
        assertThat(response.deadlineDate()).isEqualTo(d("2026-10-11"));

        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::title, ResolvedOccurrence::time)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("복습", LocalTime.of(20, 0)),
                        org.assertj.core.groups.Tuple.tuple("시험 대비", null),
                        org.assertj.core.groups.Tuple.tuple("복습", LocalTime.of(20, 0)));
    }

    @Test
    void THIS로_날짜를_옮겨도_발생일은_유지된다() {
        Long id = createDaily("2026-10-08", "2026-10-10", null);

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-09", "THIS",
                body("{\"date\": \"2026-10-15\"}"));

        assertThat(response.date()).isEqualTo(d("2026-10-15"));
        assertThat(response.occurrenceDate()).isEqualTo(d("2026-10-09"));
        assertThat(list("2026-10-01", "2026-10-31")).extracting(ResolvedOccurrence::date)
                .containsExactly(d("2026-10-08"), d("2026-10-10"), d("2026-10-15"));
    }

    @Test
    void THIS는_반복_설정을_바꿀_수_없다() {
        Long id = createDaily("2026-10-08", "2026-10-10", null);
        assertError(() -> updateService.update(userId, id, "2026-10-09", "THIS",
                body("{\"recurrence\": {\"frequency\": \"WEEKLY\"}}")), ErrorCode.INVALID_UPDATE_SCOPE);
    }

    // ===== ALL_FUTURE =====

    @Test
    void 첫_회차부터_ALL_FUTURE는_시리즈_전체를_수정하고_개별_수정은_초기화() {
        Long id = createDaily("2026-10-08", "2026-10-10", null);
        saveState(id, "2026-10-09", Map.of("overrideTitle", "따로 바꾼 제목", "isCompleted", true));

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-08", "ALL_FUTURE",
                body("{\"title\": \"영어 복습\"}"));

        assertThat(response.scheduleId()).isEqualTo(id);
        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::scheduleId, ResolvedOccurrence::title, ResolvedOccurrence::completed)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(id, "영어 복습", false),
                        org.assertj.core.groups.Tuple.tuple(id, "영어 복습", true),
                        org.assertj.core.groups.Tuple.tuple(id, "영어 복습", false));
    }

    @Test
    void 중간_회차부터_ALL_FUTURE는_시리즈를_나눈다() {
        Long id = createDaily("2026-10-08", "2026-10-12", null);
        saveState(id, "2026-10-08", Map.of("isCompleted", true));
        saveState(id, "2026-10-11", Map.of("isCompleted", true));
        saveState(id, "2026-10-12", Map.of("overrideTitle", "개별 수정"));

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-10", "ALL_FUTURE",
                body("{\"title\": \"새 복습\", \"priority\": \"HIGH\"}"));

        Long newId = response.scheduleId();
        assertThat(newId).isNotEqualTo(id);
        assertThat(response.occurrenceDate()).isEqualTo(d("2026-10-10"));
        assertThat(response.recurrence().endDate()).isEqualTo(d("2026-10-12"));

        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::scheduleId, ResolvedOccurrence::date,
                        ResolvedOccurrence::title, ResolvedOccurrence::completed)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(id, d("2026-10-08"), "복습", true),
                        org.assertj.core.groups.Tuple.tuple(id, d("2026-10-09"), "복습", false),
                        org.assertj.core.groups.Tuple.tuple(newId, d("2026-10-10"), "새 복습", false),
                        org.assertj.core.groups.Tuple.tuple(newId, d("2026-10-11"), "새 복습", true),
                        org.assertj.core.groups.Tuple.tuple(newId, d("2026-10-12"), "새 복습", false));

        ScheduleRecurrenceRule oldRule = ruleRepository.findById(id).orElseThrow();
        assertThat(oldRule.getRepeatEndDate()).isEqualTo(d("2026-10-09"));
        assertThat(occurrenceRepository.findAllFrom(id, d("2026-10-10"))).isEmpty();
    }

    @Test
    void 횟수_기준_반복을_나누면_새_시리즈는_남은_횟수() {
        Long id = createDaily("2026-10-08", null, 5);

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-10", "ALL_FUTURE",
                body("{\"title\": \"새 복습\"}"));

        assertThat(response.recurrence().count()).isEqualTo(3);
        ScheduleRecurrenceRule oldRule = ruleRepository.findById(id).orElseThrow();
        assertThat(oldRule.getRepeatCount()).isNull();
        assertThat(oldRule.getRepeatEndDate()).isEqualTo(d("2026-10-09"));
        assertThat(list("2026-10-01", "2026-10-31")).hasSize(5);
    }

    @Test
    void 종료_조건_없는_반복도_나눌_수_있다() {
        Long id = createDaily("2026-10-08", null, null);

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-10", "ALL_FUTURE",
                body("{\"time\": \"07:00\"}"));

        assertThat(response.recurrence().endDate()).isNull();
        assertThat(response.recurrence().count()).isNull();
        assertThat(list("2026-10-08", "2026-10-12")).extracting(ResolvedOccurrence::time)
                .containsExactly(LocalTime.of(20, 0), LocalTime.of(20, 0),
                        LocalTime.of(7, 0), LocalTime.of(7, 0), LocalTime.of(7, 0));
    }

    @Test
    void ALL_FUTURE로_반복_설정을_바꿀_수_있다() {
        Long id = createDaily("2026-10-08", "2026-10-31", null);

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-10", "ALL_FUTURE",
                body("{\"recurrence\": {\"frequency\": \"WEEKLY\", \"endDate\": \"2026-10-31\"}}"));

        assertThat(response.recurrence().frequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(list("2026-10-01", "2026-10-31")).extracting(ResolvedOccurrence::date)
                .containsExactly(d("2026-10-08"), d("2026-10-09"),
                        d("2026-10-10"), d("2026-10-17"), d("2026-10-24"), d("2026-10-31"));
    }

    @Test
    void ALL_FUTURE에서_반복을_해제하면_선택한_회차만_일반_일정으로_남는다() {
        Long id = createDaily("2026-10-08", "2026-10-12", null);

        ScheduleUpdateResponse response = updateService.update(userId, id, "2026-10-10", "ALL_FUTURE",
                body("{\"recurrence\": null}"));

        assertThat(response.recurrence()).isNull();
        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::date, ResolvedOccurrence::recurring)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(d("2026-10-08"), true),
                        org.assertj.core.groups.Tuple.tuple(d("2026-10-09"), true),
                        org.assertj.core.groups.Tuple.tuple(d("2026-10-10"), false));
    }

    @Test
    void 첫_회차에서_반복을_해제하면_일반_일정이_된다() {
        Long id = createDaily("2026-10-08", "2026-10-12", null);
        saveState(id, "2026-10-08", Map.of("isCompleted", true));
        saveState(id, "2026-10-09", Map.of("isCompleted", true));

        updateService.update(userId, id, "2026-10-08", "ALL_FUTURE", body("{\"recurrence\": null}"));

        assertThat(ruleRepository.findById(id)).isEmpty();
        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::date, ResolvedOccurrence::completed, ResolvedOccurrence::recurring)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(d("2026-10-08"), true, false));
        assertThat(occurrenceRepository.findAllFrom(id, d("2026-10-09"))).isEmpty();
    }

    @Test
    void 기존_종료일보다_늦게_옮기면_INVALID_RECURRENCE() {
        Long id = createDaily("2026-10-08", "2026-10-12", null);
        assertError(() -> updateService.update(userId, id, "2026-10-10", "ALL_FUTURE",
                body("{\"date\": \"2026-10-20\"}")), ErrorCode.INVALID_RECURRENCE);
    }
}
