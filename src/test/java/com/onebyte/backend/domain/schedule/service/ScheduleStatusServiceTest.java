package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.RecurrenceRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCompletionRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCompletionResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrenceId;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 완료·삭제는 회차 상태 저장과 관련 데이터 삭제가 핵심이라 로컬 MySQL로 검증한다 (contextLoads와 같은 환경) */
@SpringBootTest
@Transactional
class ScheduleStatusServiceTest {

    @Autowired ScheduleStatusService statusService;
    @Autowired ScheduleService scheduleService;
    @Autowired ScheduleEventRepository scheduleEventRepository;
    @Autowired ScheduleRecurrenceRuleRepository ruleRepository;
    @Autowired ScheduleOccurrenceRepository occurrenceRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager em;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(User.create("status-test@test.com", "tester", "pw")).getId();
    }

    private static LocalDate d(String date) {
        return LocalDate.parse(date);
    }

    private Long createNormal(String date) {
        return scheduleService.create(userId,
                new ScheduleCreateRequest("과제", date, "14:00", null, "HIGH", null)).scheduleId();
    }

    private Long createDaily(String date, String endDate) {
        return scheduleService.create(userId, new ScheduleCreateRequest(
                "복습", date, null, null, null, new RecurrenceRequest("DAILY", endDate, null))).scheduleId();
    }

    private ScheduleCompletionResponse complete(Long id, String occurrenceDate, Boolean completed) {
        return statusService.changeCompletion(userId, id, occurrenceDate, new ScheduleCompletionRequest(completed));
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

    // ===== 완료 / 완료 취소 =====

    @Test
    void 일반_일정_완료와_완료_취소() {
        Long id = createNormal("2026-10-08");

        ScheduleCompletionResponse done = complete(id, null, true);
        assertThat(done.scheduleId()).isEqualTo(id);
        assertThat(done.occurrenceDate()).isEqualTo(d("2026-10-08"));
        assertThat(done.completed()).isTrue();
        assertThat(done.updatedAt()).isNotNull();
        assertThat(list("2026-10-08", "2026-10-08")).extracting(ResolvedOccurrence::completed).containsExactly(true);

        ScheduleCompletionResponse undone = complete(id, null, false);
        assertThat(undone.completed()).isFalse();
        assertThat(list("2026-10-08", "2026-10-08")).extracting(ResolvedOccurrence::completed).containsExactly(false);
        assertThat(occurrenceRepository.findById(new ScheduleOccurrenceId(id, d("2026-10-08")))
                .orElseThrow().getCompletedAt()).isNull();
    }

    @Test
    void 같은_값을_다시_보내도_성공하고_완료_시각은_유지() {
        Long id = createNormal("2026-10-08");
        complete(id, null, true);
        em.flush();
        var firstCompletedAt = occurrenceRepository.findById(new ScheduleOccurrenceId(id, d("2026-10-08")))
                .orElseThrow().getCompletedAt();

        assertThat(complete(id, null, true).completed()).isTrue();
        assertThat(occurrenceRepository.findById(new ScheduleOccurrenceId(id, d("2026-10-08")))
                .orElseThrow().getCompletedAt()).isEqualTo(firstCompletedAt);
    }

    @Test
    void 완료한_적_없는_회차의_완료_취소는_상태를_만들지_않는다() {
        Long id = createNormal("2026-10-08");

        assertThat(complete(id, null, false).completed()).isFalse();
        assertThat(occurrenceRepository.findById(new ScheduleOccurrenceId(id, d("2026-10-08")))).isEmpty();
    }

    @Test
    void 반복_일정은_회차별로_완료를_관리한다() {
        Long id = createDaily("2026-10-08", "2026-10-10");

        complete(id, "2026-10-08", true);

        assertThat(list("2026-10-01", "2026-10-31")).extracting(ResolvedOccurrence::completed)
                .containsExactly(true, false, false);
    }

    @Test
    void 완료_요청_검증() {
        Long normal = createNormal("2026-10-08");
        Long daily = createDaily("2026-10-08", "2026-10-10");

        assertError(() -> complete(normal, null, null), ErrorCode.INVALID_INPUT);
        assertError(() -> complete(daily, null, true), ErrorCode.INVALID_INPUT);
        assertError(() -> complete(daily, "2026/10/08", true), ErrorCode.INVALID_DATE_FORMAT);
        assertError(() -> complete(daily, "2026-10-20", true), ErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> complete(normal, "2026-10-09", true), ErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> complete(-1L, null, true), ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    void 다른_사용자의_일정은_완료할_수_없다() {
        Long id = createNormal("2026-10-08");
        Long otherUserId = userRepository.save(User.create("other@test.com", "other", "pw")).getId();

        assertError(() -> statusService.changeCompletion(otherUserId, id, null, new ScheduleCompletionRequest(true)),
                ErrorCode.SCHEDULE_NOT_FOUND);
    }

    // ===== 삭제 =====

    @Test
    void 일반_일정_삭제는_일정과_회차_상태를_지운다() {
        Long id = createNormal("2026-10-08");
        complete(id, null, true);

        statusService.delete(userId, id, null, null);

        em.flush();
        em.clear();
        assertThat(scheduleEventRepository.findById(id)).isEmpty();
        assertThat(occurrenceRepository.findAllFrom(id, d("2026-01-01"))).isEmpty();
        assertThat(list("2026-10-01", "2026-10-31")).isEmpty();
    }

    @Test
    void 일반_일정에_삭제_범위를_지정하면_400() {
        Long id = createNormal("2026-10-08");
        assertError(() -> statusService.delete(userId, id, "THIS", "2026-10-08"), ErrorCode.INVALID_DELETE_SCOPE);
        assertError(() -> statusService.delete(userId, id, "ALL", null), ErrorCode.INVALID_DELETE_SCOPE);
    }

    @Test
    void THIS_삭제는_선택한_회차만_지우고_반복_규칙은_유지() {
        Long id = createDaily("2026-10-08", "2026-10-10");

        statusService.delete(userId, id, "THIS", "2026-10-09");

        assertThat(list("2026-10-01", "2026-10-31")).extracting(ResolvedOccurrence::occurrenceDate)
                .containsExactly(d("2026-10-08"), d("2026-10-10"));
        assertThat(ruleRepository.findById(id)).isPresent();
        assertThatThrownBy(() -> scheduleService.getDetail(userId, id, "2026-10-09"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 이미_삭제한_회차를_다시_삭제하면_404() {
        Long id = createDaily("2026-10-08", "2026-10-10");
        statusService.delete(userId, id, "THIS", "2026-10-09");

        assertError(() -> statusService.delete(userId, id, "THIS", "2026-10-09"), ErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> complete(id, "2026-10-09", true), ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    void ALL_삭제는_일정_반복_규칙_회차_상태를_모두_지운다() {
        Long id = createDaily("2026-10-08", "2026-10-10");
        complete(id, "2026-10-08", true);
        statusService.delete(userId, id, "THIS", "2026-10-09");

        statusService.delete(userId, id, "ALL", null);

        em.flush();
        em.clear();
        assertThat(scheduleEventRepository.findById(id)).isEmpty();
        assertThat(ruleRepository.findById(id)).isEmpty();
        assertThat(occurrenceRepository.findAllFrom(id, d("2026-01-01"))).isEmpty();
        assertThat(list("2026-10-01", "2026-10-31")).isEmpty();
    }

    @Test
    void 반복_일정_삭제_범위_검증() {
        Long id = createDaily("2026-10-08", "2026-10-10");

        assertError(() -> statusService.delete(userId, id, null, null), ErrorCode.INVALID_DELETE_SCOPE);
        assertError(() -> statusService.delete(userId, id, "ALL_FUTURE", null), ErrorCode.INVALID_DELETE_SCOPE);
        assertError(() -> statusService.delete(userId, id, "THIS", null), ErrorCode.INVALID_DELETE_SCOPE);
        assertError(() -> statusService.delete(userId, id, "THIS", "2026-10-20"), ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    void 없는_일정이나_남의_일정은_삭제할_수_없다() {
        Long id = createNormal("2026-10-08");
        Long otherUserId = userRepository.save(User.create("other2@test.com", "other", "pw")).getId();

        assertError(() -> statusService.delete(otherUserId, id, null, null), ErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> statusService.delete(userId, -1L, null, null), ErrorCode.SCHEDULE_NOT_FOUND);
        assertThat(scheduleEventRepository.findById(id)).isPresent();
    }
}
