package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.RecurrenceRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleRequestParserTest {

    private final ScheduleRequestParser parser = new ScheduleRequestParser();

    private static ScheduleCreateRequest request(String title, String date, String time, String deadline,
                                                 String priority, RecurrenceRequest recurrence) {
        return new ScheduleCreateRequest(title, date, time, deadline, priority, recurrence);
    }

    private static void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(code);
    }

    @Test
    void 일반_일정_정상_변환() {
        ScheduleCreateCommand command = parser.parseCreate(
                request("  데이터베이스 과제 제출  ", "2026-10-08", "14:00", "2026-10-10", "HIGH", null));

        assertThat(command).isEqualTo(new ScheduleCreateCommand(
                "데이터베이스 과제 제출", LocalDate.of(2026, 10, 8), LocalTime.of(14, 0),
                LocalDate.of(2026, 10, 10), SchedulePriority.HIGH, null));
    }

    @Test
    void 선택_값_누락_시_기본값() {
        ScheduleCreateCommand command = parser.parseCreate(request("복습", "2026-10-08", null, null, null, null));

        assertThat(command.time()).isNull();
        assertThat(command.deadlineDate()).isNull();
        assertThat(command.priority()).isEqualTo(SchedulePriority.MEDIUM);
        assertThat(command.recurrence()).isNull();
    }

    @Test
    void 반복_일정_정상_변환() {
        ScheduleCreateCommand command = parser.parseCreate(request("영어 단어 복습", "2026-10-08", null, null, "MEDIUM",
                new RecurrenceRequest("DAILY", "2026-10-31", null)));

        assertThat(command.recurrence()).isEqualTo(
                new ScheduleCreateCommand.Recurrence(RecurrenceFrequency.DAILY, LocalDate.of(2026, 10, 31), null));
    }

    @Test
    void 종료_조건_없는_반복_허용() {
        ScheduleCreateCommand command = parser.parseCreate(request("복습", "2026-10-08", null, null, null,
                new RecurrenceRequest("WEEKLY", null, null)));

        assertThat(command.recurrence()).isEqualTo(
                new ScheduleCreateCommand.Recurrence(RecurrenceFrequency.WEEKLY, null, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void 제목_누락_또는_공백이면_TITLE_REQUIRED(String title) {
        assertError(() -> parser.parseCreate(request(title, "2026-10-08", null, null, null, null)),
                ErrorCode.TITLE_REQUIRED);
    }

    @Test
    void 제목은_100자까지_허용() {
        String title100 = "가".repeat(100);
        assertThat(parser.parseTitle(title100)).hasSize(100);
        assertError(() -> parser.parseTitle(title100 + "가"), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 날짜_누락은_INVALID_INPUT() {
        assertError(() -> parser.parseCreate(request("복습", null, null, null, null, null)), ErrorCode.INVALID_INPUT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026/10/08", "2026-13-01", "2026-02-30", "26-10-08", "abc"})
    void 날짜_형식_오류는_INVALID_DATE_FORMAT(String date) {
        assertError(() -> parser.parseCreate(request("복습", date, null, null, null, null)),
                ErrorCode.INVALID_DATE_FORMAT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"25:00", "14:00:00", "2pm", "14-00"})
    void 시간_형식_오류는_INVALID_DATE_FORMAT(String time) {
        assertError(() -> parser.parseCreate(request("복습", "2026-10-08", time, null, null, null)),
                ErrorCode.INVALID_DATE_FORMAT);
    }

    @Test
    void 마감일이_일정_날짜보다_빠르면_INVALID_INPUT() {
        assertError(() -> parser.parseCreate(request("복습", "2026-10-08", null, "2026-10-07", null, null)),
                ErrorCode.INVALID_INPUT);
        assertThat(parser.parseDeadline("2026-10-08", LocalDate.of(2026, 10, 8))).isEqualTo(LocalDate.of(2026, 10, 8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"URGENT", "high"})
    void 우선순위_값_오류는_INVALID_INPUT(String priority) {
        assertError(() -> parser.parseCreate(request("복습", "2026-10-08", null, null, priority, null)),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 반복_주기_누락_또는_오류는_INVALID_RECURRENCE() {
        LocalDate start = LocalDate.of(2026, 10, 8);
        assertError(() -> parser.parseRecurrence(new RecurrenceRequest(null, null, null), start),
                ErrorCode.INVALID_RECURRENCE);
        assertError(() -> parser.parseRecurrence(new RecurrenceRequest("YEARLY", null, null), start),
                ErrorCode.INVALID_RECURRENCE);
    }

    @Test
    void 반복_종료일이_시작일보다_빠르면_INVALID_RECURRENCE() {
        assertError(() -> parser.parseRecurrence(
                new RecurrenceRequest("DAILY", "2026-10-07", null), LocalDate.of(2026, 10, 8)),
                ErrorCode.INVALID_RECURRENCE);
    }

    @Test
    void 반복_횟수가_1_미만이면_INVALID_RECURRENCE() {
        assertError(() -> parser.parseRecurrence(
                new RecurrenceRequest("DAILY", null, 0), LocalDate.of(2026, 10, 8)),
                ErrorCode.INVALID_RECURRENCE);
    }

    @Test
    void 종료일과_횟수_동시_지정은_INVALID_RECURRENCE() {
        assertError(() -> parser.parseRecurrence(
                new RecurrenceRequest("DAILY", "2026-10-31", 10), LocalDate.of(2026, 10, 8)),
                ErrorCode.INVALID_RECURRENCE);
    }

    @Test
    void 반복_종료일_형식_오류는_INVALID_DATE_FORMAT() {
        assertError(() -> parser.parseRecurrence(
                new RecurrenceRequest("DAILY", "2026.10.31", null), LocalDate.of(2026, 10, 8)),
                ErrorCode.INVALID_DATE_FORMAT);
    }
}
