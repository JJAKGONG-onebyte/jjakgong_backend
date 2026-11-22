package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.RecurrenceRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCompletionRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleUpdateRequest;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/** 반복 일정 경계 조건을 등록 → 수정 → 완료·삭제 → 조회 전체 흐름으로 검증한다 (로컬 MySQL) */
@SpringBootTest
@Transactional
class RecurrenceBoundaryTest {

    @Autowired ScheduleService scheduleService;
    @Autowired ScheduleUpdateService updateService;
    @Autowired ScheduleStatusService statusService;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager em;
    @Autowired ObjectMapper objectMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(User.create("boundary@test.com", "tester", "pw")).getId();
    }

    private static LocalDate d(String date) {
        return LocalDate.parse(date);
    }

    private Long create(String date, String frequency, String endDate, Integer count) {
        return scheduleService.create(userId, new ScheduleCreateRequest(
                "반복", date, null, null, null, new RecurrenceRequest(frequency, endDate, count))).scheduleId();
    }

    private List<ResolvedOccurrence> list(String from, String to) {
        em.flush();
        em.clear();
        return scheduleService.findOccurrences(userId, d(from), d(to));
    }

    private List<LocalDate> dates(String from, String to) {
        return list(from, to).stream().map(ResolvedOccurrence::date).toList();
    }

    private ScheduleUpdateRequest body(String json) {
        return objectMapper.readValue(json, ScheduleUpdateRequest.class);
    }

    @Test
    void 매월_31일은_31일이_없는_달을_건너뛰고_횟수에도_넣지_않는다() {
        create("2027-01-31", "MONTHLY", null, 4);

        assertThat(dates("2027-01-01", "2027-12-31"))
                .containsExactly(d("2027-01-31"), d("2027-03-31"), d("2027-05-31"), d("2027-07-31"));
    }

    @Test
    void 윤년_2월_29일_매월_반복은_평년_2월을_건너뛴다() {
        create("2028-01-29", "MONTHLY", "2029-03-31", null);

        assertThat(dates("2028-02-01", "2028-02-29")).containsExactly(d("2028-02-29"));
        assertThat(dates("2029-02-01", "2029-03-31")).containsExactly(d("2029-03-29"));
    }

    @Test
    void 종료일_당일과_횟수의_마지막_회차를_포함한다() {
        create("2026-10-08", "WEEKLY", "2026-10-29", null);
        create("2026-11-01", "DAILY", null, 3);

        assertThat(dates("2026-10-01", "2026-11-30")).containsExactly(
                d("2026-10-08"), d("2026-10-15"), d("2026-10-22"), d("2026-10-29"),
                d("2026-11-01"), d("2026-11-02"), d("2026-11-03"));
    }

    @Test
    void 연말연초에_걸친_조회() {
        create("2026-12-30", "DAILY", "2027-01-02", null);

        assertThat(dates("2026-12-31", "2027-01-01")).containsExactly(d("2026-12-31"), d("2027-01-01"));
    }

    @Test
    void 조회_기간은_366일까지_허용() {
        assertThat(scheduleService.getList(userId, "2028-01-01", "2028-12-31").schedules()).isEmpty();
        assertThatThrownBy(() -> scheduleService.getList(userId, "2026-01-01", "2027-01-02"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void THIS로_구간_밖에서_안으로_옮긴_회차와_안에서_밖으로_옮긴_회차() {
        Long id = create("2026-10-29", "DAILY", "2026-11-02", null);
        updateService.update(userId, id, "2026-10-30", "THIS", body("{\"date\":\"2026-11-05\"}"));
        updateService.update(userId, id, "2026-11-02", "THIS", body("{\"date\":\"2026-10-31\"}"));

        assertThat(list("2026-11-01", "2026-11-30"))
                .extracting(ResolvedOccurrence::date, ResolvedOccurrence::occurrenceDate)
                .containsExactly(
                        tuple(d("2026-11-01"), d("2026-11-01")),
                        tuple(d("2026-11-05"), d("2026-10-30")));
        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::date, ResolvedOccurrence::occurrenceDate)
                .containsExactly(
                        tuple(d("2026-10-29"), d("2026-10-29")),
                        tuple(d("2026-10-31"), d("2026-10-31")),
                        tuple(d("2026-10-31"), d("2026-11-02")));
    }

    @Test
    void 마지막_회차에서_ALL_FUTURE를_하면_마지막_회차만_새_시리즈가_된다() {
        Long id = create("2026-10-08", "DAILY", "2026-10-10", null);

        Long newId = updateService.update(userId, id, "2026-10-10", "ALL_FUTURE",
                body("{\"title\":\"마지막\"}")).scheduleId();

        assertThat(list("2026-10-01", "2026-10-31"))
                .extracting(ResolvedOccurrence::scheduleId, ResolvedOccurrence::date, ResolvedOccurrence::title)
                .containsExactly(
                        tuple(id, d("2026-10-08"), "반복"),
                        tuple(id, d("2026-10-09"), "반복"),
                        tuple(newId, d("2026-10-10"), "마지막"));
    }

    @Test
    void 건너뛴_달이_있는_횟수_기준_시리즈를_나눠도_전체_회차_수는_유지된다() {
        Long id = create("2027-01-31", "MONTHLY", null, 4);

        Long newId = updateService.update(userId, id, "2027-05-31", "ALL_FUTURE",
                body("{\"title\":\"후반\"}")).scheduleId();

        assertThat(list("2027-01-01", "2027-12-31"))
                .extracting(ResolvedOccurrence::scheduleId, ResolvedOccurrence::date)
                .containsExactly(
                        tuple(id, d("2027-01-31")),
                        tuple(id, d("2027-03-31")),
                        tuple(newId, d("2027-05-31")),
                        tuple(newId, d("2027-07-31")));
    }

    @Test
    void 반복_주기를_바꾸면_새_규칙의_발생일인_날의_상태만_유지된다() {
        Long id = create("2026-10-05", "DAILY", "2026-10-31", null);
        statusService.changeCompletion(userId, id, "2026-10-12", new ScheduleCompletionRequest(true));
        statusService.changeCompletion(userId, id, "2026-10-13", new ScheduleCompletionRequest(true));
        statusService.delete(userId, id, "THIS", "2026-10-19");

        Long newId = updateService.update(userId, id, "2026-10-12", "ALL_FUTURE",
                body("{\"recurrence\":{\"frequency\":\"WEEKLY\",\"endDate\":\"2026-10-31\"}}")).scheduleId();

        assertThat(list("2026-10-12", "2026-10-31"))
                .extracting(ResolvedOccurrence::scheduleId, ResolvedOccurrence::date, ResolvedOccurrence::completed)
                .containsExactly(
                        tuple(newId, d("2026-10-12"), true),
                        tuple(newId, d("2026-10-26"), false));
    }
}
