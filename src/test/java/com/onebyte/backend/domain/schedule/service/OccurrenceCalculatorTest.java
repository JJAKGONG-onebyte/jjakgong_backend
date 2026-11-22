package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OccurrenceCalculatorTest {

    private final OccurrenceCalculator calculator = new OccurrenceCalculator();

    private static LocalDate d(String date) {
        return LocalDate.parse(date);
    }

    private static ScheduleRecurrenceRule rule(RecurrenceFrequency frequency, LocalDate endDate, Integer count) {
        return ScheduleRecurrenceRule.builder()
                .frequency(frequency)
                .repeatEndDate(endDate)
                .repeatCount(count)
                .build();
    }

    @Nested
    @DisplayName("일반 일정")
    class NonRecurring {

        @Test
        void 구간_안이면_일정_날짜_하나() {
            assertThat(calculator.occurrencesBetween(d("2026-10-08"), null, d("2026-10-01"), d("2026-10-31")))
                    .containsExactly(d("2026-10-08"));
        }

        @Test
        void 구간_밖이면_빈_목록() {
            assertThat(calculator.occurrencesBetween(d("2026-11-01"), null, d("2026-10-01"), d("2026-10-31")))
                    .isEmpty();
        }

        @Test
        void 구간_경계일도_포함() {
            assertThat(calculator.occurrencesBetween(d("2026-10-31"), null, d("2026-10-01"), d("2026-10-31")))
                    .containsExactly(d("2026-10-31"));
        }
    }

    @Nested
    @DisplayName("DAILY")
    class Daily {

        @Test
        void 종료일까지_매일_반복하고_종료일을_포함() {
            var r = rule(RecurrenceFrequency.DAILY, d("2026-10-10"), null);
            assertThat(calculator.occurrencesBetween(d("2026-10-08"), r, d("2026-10-01"), d("2026-10-31")))
                    .containsExactly(d("2026-10-08"), d("2026-10-09"), d("2026-10-10"));
        }

        @Test
        void 횟수는_최초_일정을_포함해_센다() {
            var r = rule(RecurrenceFrequency.DAILY, null, 3);
            assertThat(calculator.occurrencesBetween(d("2026-10-08"), r, d("2026-10-01"), d("2026-10-31")))
                    .containsExactly(d("2026-10-08"), d("2026-10-09"), d("2026-10-10"));
        }

        @Test
        void 시작일이_구간_이전이면_구간_안의_회차만() {
            var r = rule(RecurrenceFrequency.DAILY, null, null);
            assertThat(calculator.occurrencesBetween(d("2026-01-01"), r, d("2026-10-30"), d("2026-11-02")))
                    .containsExactly(d("2026-10-30"), d("2026-10-31"), d("2026-11-01"), d("2026-11-02"));
        }

        @Test
        void 횟수가_구간_이전에_끝나면_빈_목록() {
            var r = rule(RecurrenceFrequency.DAILY, null, 5);
            assertThat(calculator.occurrencesBetween(d("2026-10-01"), r, d("2026-10-06"), d("2026-10-31")))
                    .isEmpty();
        }

        @Test
        void 종료_조건이_없으면_구간_끝까지() {
            var r = rule(RecurrenceFrequency.DAILY, null, null);
            assertThat(calculator.occurrencesBetween(d("2026-10-01"), r, d("2026-10-01"), d("2026-10-31")))
                    .hasSize(31);
        }
    }

    @Nested
    @DisplayName("WEEKLY")
    class Weekly {

        @Test
        void 시작일과_같은_요일에_반복() {
            var r = rule(RecurrenceFrequency.WEEKLY, null, null);
            // 2026-10-08은 목요일
            assertThat(calculator.occurrencesBetween(d("2026-10-08"), r, d("2026-10-01"), d("2026-10-31")))
                    .containsExactly(d("2026-10-08"), d("2026-10-15"), d("2026-10-22"), d("2026-10-29"));
        }

        @Test
        void 구간_시작이_회차_사이면_다음_회차부터() {
            var r = rule(RecurrenceFrequency.WEEKLY, null, null);
            assertThat(calculator.occurrencesBetween(d("2026-10-08"), r, d("2026-10-16"), d("2026-10-31")))
                    .containsExactly(d("2026-10-22"), d("2026-10-29"));
        }

        @Test
        void 횟수_제한() {
            var r = rule(RecurrenceFrequency.WEEKLY, null, 2);
            assertThat(calculator.occurrencesBetween(d("2026-10-08"), r, d("2026-10-01"), d("2026-12-31")))
                    .containsExactly(d("2026-10-08"), d("2026-10-15"));
        }
    }

    @Nested
    @DisplayName("MONTHLY")
    class Monthly {

        @Test
        void 매월_같은_날짜에_반복() {
            var r = rule(RecurrenceFrequency.MONTHLY, d("2026-12-31"), null);
            assertThat(calculator.occurrencesBetween(d("2026-10-15"), r, d("2026-01-01"), d("2027-12-31")))
                    .containsExactly(d("2026-10-15"), d("2026-11-15"), d("2026-12-15"));
        }

        @Test
        void 날짜가_없는_달은_건너뛴다() {
            var r = rule(RecurrenceFrequency.MONTHLY, null, null);
            assertThat(calculator.occurrencesBetween(d("2027-01-31"), r, d("2027-01-01"), d("2027-05-31")))
                    .containsExactly(d("2027-01-31"), d("2027-03-31"), d("2027-05-31"));
        }

        @Test
        void 건너뛴_달은_횟수에_포함하지_않는다() {
            var r = rule(RecurrenceFrequency.MONTHLY, null, 3);
            assertThat(calculator.occurrencesBetween(d("2027-01-31"), r, d("2027-01-01"), d("2027-12-31")))
                    .containsExactly(d("2027-01-31"), d("2027-03-31"), d("2027-05-31"));
        }

        @Test
        void 윤년_2월_29일() {
            var r = rule(RecurrenceFrequency.MONTHLY, null, null);
            assertThat(calculator.occurrencesBetween(d("2028-01-29"), r, d("2028-02-01"), d("2028-02-29")))
                    .containsExactly(d("2028-02-29"));
        }

        @Test
        void 횟수가_구간_이전에_소진되면_빈_목록() {
            var r = rule(RecurrenceFrequency.MONTHLY, null, 2);
            assertThat(calculator.occurrencesBetween(d("2026-01-10"), r, d("2026-03-01"), d("2026-12-31")))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("경계 조건")
    class Edge {

        @Test
        void 시작일이_구간_이후면_빈_목록() {
            var r = rule(RecurrenceFrequency.DAILY, null, null);
            assertThat(calculator.occurrencesBetween(d("2026-11-05"), r, d("2026-10-01"), d("2026-10-31")))
                    .isEmpty();
        }

        @Test
        void 종료일이_구간_이전이면_빈_목록() {
            var r = rule(RecurrenceFrequency.DAILY, d("2026-09-30"), null);
            assertThat(calculator.occurrencesBetween(d("2026-09-01"), r, d("2026-10-01"), d("2026-10-31")))
                    .isEmpty();
        }

        @Test
        void 구간_시작이_종료보다_늦으면_빈_목록() {
            var r = rule(RecurrenceFrequency.DAILY, null, null);
            assertThat(calculator.occurrencesBetween(d("2026-10-01"), r, d("2026-10-31"), d("2026-10-01")))
                    .isEmpty();
        }

        @Test
        void 발생일_여부_확인() {
            var r = rule(RecurrenceFrequency.WEEKLY, null, null);
            assertThat(calculator.isOccurrence(d("2026-10-08"), r, d("2026-10-15"))).isTrue();
            assertThat(calculator.isOccurrence(d("2026-10-08"), r, d("2026-10-16"))).isFalse();
            assertThat(calculator.isOccurrence(d("2026-10-08"), r, d("2026-10-01"))).isFalse();
            assertThat(calculator.isOccurrence(d("2026-10-08"), null, d("2026-10-08"))).isTrue();
        }
    }
}
