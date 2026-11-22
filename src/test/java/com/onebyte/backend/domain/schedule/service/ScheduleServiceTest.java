package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.RecurrenceRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleDetailResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleListResponse;
import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.ScheduleEventType;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import com.onebyte.backend.domain.schedule.repository.ScheduleEventRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleOccurrenceRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleRecurrenceRuleRepository;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final Long USER_ID = 1L;

    @Mock ScheduleEventRepository scheduleEventRepository;
    @Mock ScheduleRecurrenceRuleRepository recurrenceRuleRepository;
    @Mock ScheduleOccurrenceRepository occurrenceRepository;
    @Mock UserRepository userRepository;

    private ScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(scheduleEventRepository, recurrenceRuleRepository, occurrenceRepository,
                userRepository, new ScheduleOccurrenceResolver(new OccurrenceCalculator()), new ScheduleRequestParser());
    }

    private static LocalDate d(String date) {
        return LocalDate.parse(date);
    }

    private static ScheduleEvent schedule(long id, LocalDate date) {
        ScheduleEvent event = ScheduleEvent.builder().title("복습").scheduleDate(date).build();
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private static void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(code);
    }

    // ===== 등록 =====

    @Test
    void 일반_일정_등록은_반복_규칙을_저장하지_않는다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.create("a@a.com", "a", "pw")));
        when(scheduleEventRepository.save(any())).thenAnswer(inv -> {
            ScheduleEvent e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 104L);
            return e;
        });

        ScheduleCreateResponse response = service.create(USER_ID, new ScheduleCreateRequest(
                "데이터베이스 과제 제출", "2026-10-08", "14:00", "2026-10-10", "HIGH", null));

        assertThat(response.scheduleId()).isEqualTo(104L);
        assertThat(response.time()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.priority()).isEqualTo(SchedulePriority.HIGH);
        assertThat(response.completed()).isFalse();
        assertThat(response.recurrence()).isNull();
        verify(recurrenceRuleRepository, never()).save(any());
    }

    @Test
    void 반복_일정_등록은_일정과_반복_규칙을_함께_저장한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.create("a@a.com", "a", "pw")));
        when(scheduleEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recurrenceRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScheduleCreateResponse response = service.create(USER_ID, new ScheduleCreateRequest(
                "영어 단어 복습", "2026-10-08", null, null, null, new RecurrenceRequest("DAILY", null, 10)));

        ArgumentCaptor<ScheduleEvent> eventCaptor = ArgumentCaptor.forClass(ScheduleEvent.class);
        verify(scheduleEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(ScheduleEventType.EVENT);
        assertThat(eventCaptor.getValue().getPriority()).isEqualTo(SchedulePriority.MEDIUM);
        assertThat(eventCaptor.getValue().getScheduleTime()).isNull();

        ArgumentCaptor<ScheduleRecurrenceRule> ruleCaptor = ArgumentCaptor.forClass(ScheduleRecurrenceRule.class);
        verify(recurrenceRuleRepository).save(ruleCaptor.capture());
        ScheduleRecurrenceRule rule = ruleCaptor.getValue();
        assertThat(rule.getSchedule()).isSameAs(eventCaptor.getValue());
        assertThat(rule.getFrequency()).isEqualTo(RecurrenceFrequency.DAILY);
        assertThat(rule.getRepeatInterval()).isEqualTo(1);
        assertThat(rule.getRepeatCount()).isEqualTo(10);
        assertThat(rule.getRepeatEndDate()).isNull();

        assertThat(response.recurrence().frequency()).isEqualTo(RecurrenceFrequency.DAILY);
        assertThat(response.recurrence().count()).isEqualTo(10);
    }

    @Test
    void 입력값_오류면_저장하지_않는다() {
        assertError(() -> service.create(USER_ID, new ScheduleCreateRequest(
                " ", "2026-10-08", null, null, null, null)), ErrorCode.TITLE_REQUIRED);
        verifyNoInteractions(scheduleEventRepository, recurrenceRuleRepository, userRepository);
    }

    // ===== 목록 =====

    @Test
    void 목록은_반복_일정을_발생일별로_펼쳐_정렬해_반환한다() {
        ScheduleEvent once = schedule(1L, d("2026-10-09"));
        ReflectionTestUtils.setField(once, "scheduleTime", LocalTime.of(9, 0));
        ScheduleEvent daily = schedule(2L, d("2026-10-08"));
        ScheduleRecurrenceRule rule = ScheduleRecurrenceRule.builder()
                .schedule(daily).frequency(RecurrenceFrequency.DAILY).repeatEndDate(d("2026-10-09")).build();
        ReflectionTestUtils.setField(rule, "scheduleId", 2L);
        ScheduleOccurrence done = ScheduleOccurrence.of(daily, d("2026-10-08"));
        ReflectionTestUtils.setField(done, "isCompleted", true);

        when(scheduleEventRepository.findCandidatesInRange(USER_ID, d("2026-10-01"), d("2026-10-31")))
                .thenReturn(List.of(once, daily));
        when(recurrenceRuleRepository.findAllByScheduleIdsAndUserId(List.of(1L, 2L), USER_ID))
                .thenReturn(List.of(rule));
        when(occurrenceRepository.findAllInRange(List.of(1L, 2L), USER_ID, d("2026-10-01"), d("2026-10-31")))
                .thenReturn(List.of(done));

        ScheduleListResponse response = service.getList(USER_ID, "2026-10-01", "2026-10-31");

        assertThat(response.startDate()).isEqualTo(d("2026-10-01"));
        assertThat(response.schedules())
                .extracting(ScheduleListResponse.Item::scheduleId, ScheduleListResponse.Item::date,
                        ScheduleListResponse.Item::completed, ScheduleListResponse.Item::isRecurring)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, d("2026-10-08"), true, true),
                        org.assertj.core.groups.Tuple.tuple(2L, d("2026-10-09"), false, true),
                        org.assertj.core.groups.Tuple.tuple(1L, d("2026-10-09"), false, false));
    }

    @Test
    void 일정이_없으면_빈_목록이고_추가_조회를_하지_않는다() {
        when(scheduleEventRepository.findCandidatesInRange(USER_ID, d("2026-10-01"), d("2026-10-31")))
                .thenReturn(List.of());

        assertThat(service.getList(USER_ID, "2026-10-01", "2026-10-31").schedules()).isEmpty();
        verifyNoInteractions(recurrenceRuleRepository, occurrenceRepository);
    }

    @Test
    void 시작일이_종료일보다_늦으면_INVALID_DATE_RANGE() {
        assertError(() -> service.getList(USER_ID, "2026-10-31", "2026-10-01"), ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void 조회_기간은_최대_366일() {
        when(scheduleEventRepository.findCandidatesInRange(anyLong(), any(), any())).thenReturn(List.of());
        assertThat(service.getList(USER_ID, "2028-01-01", "2028-12-31").schedules()).isEmpty();

        assertError(() -> service.getList(USER_ID, "2026-01-01", "2027-01-02"), ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void 조회_날짜_형식_오류는_INVALID_DATE_FORMAT() {
        assertError(() -> service.getList(USER_ID, "2026/10/01", "2026-10-31"), ErrorCode.INVALID_DATE_FORMAT);
    }

    // ===== 상세 =====

    @Test
    void 일반_일정은_발생일을_생략하면_일정_날짜로_조회() {
        ScheduleEvent once = schedule(1L, d("2026-10-08"));
        when(scheduleEventRepository.findByIdAndUser_Id(1L, USER_ID)).thenReturn(Optional.of(once));
        when(recurrenceRuleRepository.findByScheduleIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());
        when(occurrenceRepository.findOne(1L, d("2026-10-08"), USER_ID)).thenReturn(Optional.empty());

        ScheduleDetailResponse response = service.getDetail(USER_ID, 1L, null);

        assertThat(response.occurrenceDate()).isEqualTo(d("2026-10-08"));
        assertThat(response.recurrence()).isNull();
        assertThat(response.completed()).isFalse();
    }

    @Test
    void 일반_일정에_다른_발생일을_지정하면_404() {
        ScheduleEvent once = schedule(1L, d("2026-10-08"));
        when(scheduleEventRepository.findByIdAndUser_Id(1L, USER_ID)).thenReturn(Optional.of(once));
        when(recurrenceRuleRepository.findByScheduleIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertError(() -> service.getDetail(USER_ID, 1L, "2026-10-09"), ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    void 반복_일정은_발생일이_없으면_400() {
        ScheduleEvent daily = schedule(2L, d("2026-10-08"));
        ScheduleRecurrenceRule rule = ScheduleRecurrenceRule.builder()
                .schedule(daily).frequency(RecurrenceFrequency.DAILY).build();
        when(scheduleEventRepository.findByIdAndUser_Id(2L, USER_ID)).thenReturn(Optional.of(daily));
        when(recurrenceRuleRepository.findByScheduleIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(rule));

        assertError(() -> service.getDetail(USER_ID, 2L, null), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 반복_일정의_회차_상세는_반복_설정과_회차_상태를_반환() {
        ScheduleEvent weekly = schedule(2L, d("2026-10-08"));
        ScheduleRecurrenceRule rule = ScheduleRecurrenceRule.builder()
                .schedule(weekly).frequency(RecurrenceFrequency.WEEKLY).repeatEndDate(d("2026-12-31")).build();
        ScheduleOccurrence done = ScheduleOccurrence.of(weekly, d("2026-10-15"));
        ReflectionTestUtils.setField(done, "isCompleted", true);
        when(scheduleEventRepository.findByIdAndUser_Id(2L, USER_ID)).thenReturn(Optional.of(weekly));
        when(recurrenceRuleRepository.findByScheduleIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(rule));
        when(occurrenceRepository.findOne(2L, d("2026-10-15"), USER_ID)).thenReturn(Optional.of(done));

        ScheduleDetailResponse response = service.getDetail(USER_ID, 2L, "2026-10-15");

        assertThat(response.occurrenceDate()).isEqualTo(d("2026-10-15"));
        assertThat(response.completed()).isTrue();
        assertThat(response.recurrence().frequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(response.recurrence().endDate()).isEqualTo(d("2026-12-31"));
    }

    @Test
    void 반복_일정의_발생일이_아니면_404() {
        ScheduleEvent weekly = schedule(2L, d("2026-10-08"));
        ScheduleRecurrenceRule rule = ScheduleRecurrenceRule.builder()
                .schedule(weekly).frequency(RecurrenceFrequency.WEEKLY).build();
        when(scheduleEventRepository.findByIdAndUser_Id(2L, USER_ID)).thenReturn(Optional.of(weekly));
        when(recurrenceRuleRepository.findByScheduleIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(rule));
        when(occurrenceRepository.findOne(2L, d("2026-10-16"), USER_ID)).thenReturn(Optional.empty());

        assertError(() -> service.getDetail(USER_ID, 2L, "2026-10-16"), ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    void 없는_일정이나_남의_일정은_404() {
        when(scheduleEventRepository.findByIdAndUser_Id(99L, USER_ID)).thenReturn(Optional.empty());

        assertError(() -> service.getDetail(USER_ID, 99L, null), ErrorCode.SCHEDULE_NOT_FOUND);
    }
}
