package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleOccurrenceResolverTest {

    private final ScheduleOccurrenceResolver resolver = new ScheduleOccurrenceResolver(new OccurrenceCalculator());

    private static final LocalDate FROM = LocalDate.parse("2026-10-01");
    private static final LocalDate TO = LocalDate.parse("2026-10-31");

    private static LocalDate d(String date) {
        return LocalDate.parse(date);
    }

    private static ScheduleEvent schedule(long id, String title, LocalDate date, LocalTime time,
                                          LocalDate deadline, SchedulePriority priority) {
        ScheduleEvent event = ScheduleEvent.builder()
                .title(title)
                .scheduleDate(date)
                .scheduleTime(time)
                .deadlineDate(deadline)
                .priority(priority)
                .build();
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private static ScheduleRecurrenceRule daily(ScheduleEvent schedule, LocalDate endDate) {
        return ScheduleRecurrenceRule.builder()
                .schedule(schedule)
                .frequency(RecurrenceFrequency.DAILY)
                .repeatEndDate(endDate)
                .build();
    }

    /** 회차 상태는 생성 메서드만 있어 테스트에서는 필드를 직접 채운다 */
    private static ScheduleOccurrence state(ScheduleEvent schedule, LocalDate occurrenceDate, Map<String, Object> fields) {
        ScheduleOccurrence occurrence = ScheduleOccurrence.of(schedule, occurrenceDate);
        fields.forEach((name, value) -> ReflectionTestUtils.setField(occurrence, name, value));
        return occurrence;
    }

    @Test
    void 일반_일정은_발생일이_일정_날짜이고_반복_아님() {
        var s = schedule(1L, "과제 제출", d("2026-10-08"), LocalTime.of(14, 0), d("2026-10-10"), SchedulePriority.HIGH);

        List<ResolvedOccurrence> result = resolver.resolve(List.of(s), Map.of(), List.of(), FROM, TO);

        assertThat(result).containsExactly(new ResolvedOccurrence(
                1L, "과제 제출", d("2026-10-08"), d("2026-10-08"), LocalTime.of(14, 0),
                d("2026-10-10"), SchedulePriority.HIGH, false, false));
    }

    @Test
    void 반복_일정은_회차별로_펼치고_마감일은_간격을_유지() {
        var s = schedule(2L, "영어 복습", d("2026-10-08"), null, d("2026-10-09"), SchedulePriority.MEDIUM);
        var r = daily(s, d("2026-10-10"));

        List<ResolvedOccurrence> result = resolver.resolve(List.of(s), Map.of(2L, r), List.of(), FROM, TO);

        assertThat(result).extracting(ResolvedOccurrence::occurrenceDate)
                .containsExactly(d("2026-10-08"), d("2026-10-09"), d("2026-10-10"));
        assertThat(result).extracting(ResolvedOccurrence::deadlineDate)
                .containsExactly(d("2026-10-09"), d("2026-10-10"), d("2026-10-11"));
        assertThat(result).allMatch(ResolvedOccurrence::recurring);
    }

    @Test
    void 완료_상태는_회차별로_독립() {
        var s = schedule(2L, "영어 복습", d("2026-10-08"), null, null, null);
        var r = daily(s, d("2026-10-09"));
        var done = state(s, d("2026-10-08"), Map.of("isCompleted", true));

        List<ResolvedOccurrence> result = resolver.resolve(List.of(s), Map.of(2L, r), List.of(done), FROM, TO);

        assertThat(result).extracting(ResolvedOccurrence::completed).containsExactly(true, false);
    }

    @Test
    void 일반_일정도_회차_상태로_완료_여부를_반영() {
        var s = schedule(1L, "과제", d("2026-10-08"), null, null, null);
        var done = state(s, d("2026-10-08"), Map.of("isCompleted", true));

        assertThat(resolver.resolve(List.of(s), Map.of(), List.of(done), FROM, TO))
                .extracting(ResolvedOccurrence::completed).containsExactly(true);
    }

    @Test
    void 개별_삭제된_회차는_제외() {
        var s = schedule(2L, "영어 복습", d("2026-10-08"), null, null, null);
        var r = daily(s, d("2026-10-10"));
        var deleted = state(s, d("2026-10-09"), Map.of("isDeleted", true));

        assertThat(resolver.resolve(List.of(s), Map.of(2L, r), List.of(deleted), FROM, TO))
                .extracting(ResolvedOccurrence::occurrenceDate)
                .containsExactly(d("2026-10-08"), d("2026-10-10"));
    }

    @Test
    void 개별_수정은_해당_회차에만_반영() {
        var s = schedule(2L, "영어 복습", d("2026-10-08"), LocalTime.of(20, 0), d("2026-10-08"), SchedulePriority.LOW);
        var r = daily(s, d("2026-10-09"));
        var edited = state(s, d("2026-10-09"), Map.of(
                "overrideTitle", "영어 시험 대비",
                "overridePriority", SchedulePriority.HIGH,
                "overrideAllDay", true,
                "clearDeadline", true));

        List<ResolvedOccurrence> result = resolver.resolve(List.of(s), Map.of(2L, r), List.of(edited), FROM, TO);

        assertThat(result.get(0)).isEqualTo(new ResolvedOccurrence(
                2L, "영어 복습", d("2026-10-08"), d("2026-10-08"), LocalTime.of(20, 0),
                d("2026-10-08"), SchedulePriority.LOW, false, true));
        assertThat(result.get(1)).isEqualTo(new ResolvedOccurrence(
                2L, "영어 시험 대비", d("2026-10-09"), d("2026-10-09"), null,
                null, SchedulePriority.HIGH, false, true));
    }

    @Test
    void 시간과_마감일_개별_변경() {
        var s = schedule(2L, "복습", d("2026-10-08"), null, null, null);
        var r = daily(s, d("2026-10-08"));
        var edited = state(s, d("2026-10-08"), Map.of(
                "overrideTime", LocalTime.of(9, 30),
                "overrideDeadlineDate", d("2026-10-12")));

        ResolvedOccurrence result = resolver.resolve(List.of(s), Map.of(2L, r), List.of(edited), FROM, TO).get(0);

        assertThat(result.time()).isEqualTo(LocalTime.of(9, 30));
        assertThat(result.deadlineDate()).isEqualTo(d("2026-10-12"));
    }

    @Test
    void 날짜를_옮긴_회차는_표시_날짜만_바뀌고_발생일은_유지() {
        var s = schedule(2L, "복습", d("2026-10-08"), null, null, null);
        var r = daily(s, d("2026-10-09"));
        var moved = state(s, d("2026-10-09"), Map.of("overrideDate", d("2026-10-20")));

        List<ResolvedOccurrence> result = resolver.resolve(List.of(s), Map.of(2L, r), List.of(moved), FROM, TO);

        assertThat(result).extracting(ResolvedOccurrence::date)
                .containsExactly(d("2026-10-08"), d("2026-10-20"));
        assertThat(result.get(1).occurrenceDate()).isEqualTo(d("2026-10-09"));
    }

    @Test
    void 구간_밖으로_옮긴_회차는_제외하고_구간_안으로_옮겨온_회차는_포함() {
        var s = schedule(2L, "복습", d("2026-09-29"), null, null, null);
        var r = daily(s, d("2026-10-02"));
        var movedIn = state(s, d("2026-09-30"), Map.of("overrideDate", d("2026-10-05")));
        var movedOut = state(s, d("2026-10-01"), Map.of("overrideDate", d("2026-09-15")));

        List<ResolvedOccurrence> result = resolver.resolve(
                List.of(s), Map.of(2L, r), List.of(movedIn, movedOut), FROM, TO);

        assertThat(result).extracting(ResolvedOccurrence::occurrenceDate)
                .containsExactly(d("2026-10-02"), d("2026-09-30"));
        assertThat(result).extracting(ResolvedOccurrence::date)
                .containsExactly(d("2026-10-02"), d("2026-10-05"));
    }

    @Test
    void 날짜_오름차순_종일_우선_시간_오름차순으로_정렬() {
        var late = schedule(1L, "저녁", d("2026-10-08"), LocalTime.of(20, 0), null, null);
        var allDay = schedule(2L, "종일", d("2026-10-08"), null, null, null);
        var early = schedule(3L, "아침", d("2026-10-08"), LocalTime.of(8, 0), null, null);
        var tomorrow = schedule(4L, "내일", d("2026-10-09"), null, null, null);

        assertThat(resolver.resolve(List.of(tomorrow, late, allDay, early), Map.of(), List.of(), FROM, TO))
                .extracting(ResolvedOccurrence::title)
                .containsExactly("종일", "아침", "저녁", "내일");
    }

    @Test
    void 단건_조회는_실제_발생일만_허용() {
        var s = schedule(2L, "복습", d("2026-10-08"), null, null, null);
        var r = ScheduleRecurrenceRule.builder().schedule(s).frequency(RecurrenceFrequency.WEEKLY).build();

        assertThat(resolver.resolveOne(s, r, null, d("2026-10-15"))).isPresent();
        assertThat(resolver.resolveOne(s, r, null, d("2026-10-16"))).isEmpty();
        assertThat(resolver.resolveOne(s, null, null, d("2026-10-09"))).isEmpty();
    }

    @Test
    void 단건_조회에서_삭제된_회차는_빈_값() {
        var s = schedule(2L, "복습", d("2026-10-08"), null, null, null);
        var r = daily(s, null);
        var deleted = state(s, d("2026-10-10"), Map.of("isDeleted", true));

        assertThat(resolver.resolveOne(s, r, deleted, d("2026-10-10"))).isEmpty();
    }
}
