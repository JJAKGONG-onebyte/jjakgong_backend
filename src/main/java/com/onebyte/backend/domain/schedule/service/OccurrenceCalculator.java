package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 반복 규칙으로 발생일을 계산한다. (DB를 사용하지 않는 순수 계산)
 *
 * - DAILY: 매일 / WEEKLY: 시작일과 같은 요일 / MONTHLY: 시작일과 같은 날짜
 * - MONTHLY에서 해당 날짜가 없는 달(예: 31일)은 건너뛰며, 반복 횟수에도 포함하지 않는다.
 * - repeatEndDate는 포함, repeatCount는 최초 일정을 1회로 센다.
 * - 종료 조건이 없으면 무기한 반복하되, 요청 구간 밖은 계산하지 않는다.
 */
@Component
public class OccurrenceCalculator {

    /**
     * [from, to] 구간의 발생일 (오름차순).
     *
     * @param start 일정 시작일 (첫 회차)
     * @param rule  반복 규칙. null이면 반복하지 않는 일반 일정
     */
    public List<LocalDate> occurrencesBetween(LocalDate start, ScheduleRecurrenceRule rule,
                                              LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return List.of();
        }
        if (rule == null) {
            return isWithin(start, from, to) ? List.of(start) : List.of();
        }

        LocalDate last = (rule.getRepeatEndDate() != null && rule.getRepeatEndDate().isBefore(to))
                ? rule.getRepeatEndDate()
                : to;
        int interval = Math.max(1, rule.getRepeatInterval());

        return switch (rule.getFrequency()) {
            case DAILY -> fixedStep(start, interval, rule.getRepeatCount(), from, last);
            case WEEKLY -> fixedStep(start, 7L * interval, rule.getRepeatCount(), from, last);
            case MONTHLY -> monthly(start, interval, rule.getRepeatCount(), from, last);
        };
    }

    /** 해당 날짜가 실제 발생일인지 (개별 회차 조회·수정·완료·삭제 시 검증용) */
    public boolean isOccurrence(LocalDate start, ScheduleRecurrenceRule rule, LocalDate date) {
        return occurrencesBetween(start, rule, date, date).contains(date);
    }

    /** 일정 간격(일)으로 반복. 건너뛰는 날이 없으므로 구간 시작 회차를 바로 계산한다. */
    private List<LocalDate> fixedStep(LocalDate start, long stepDays, Integer count, LocalDate from, LocalDate last) {
        List<LocalDate> result = new ArrayList<>();
        long index = 0;
        if (from.isAfter(start)) {
            long daysToFrom = ChronoUnit.DAYS.between(start, from);
            index = (daysToFrom + stepDays - 1) / stepDays;
        }
        while (count == null || index < count) {
            LocalDate date = start.plusDays(index * stepDays);
            if (date.isAfter(last)) {
                break;
            }
            result.add(date);
            index++;
        }
        return result;
    }

    /** 매월 같은 날짜. 그 날짜가 없는 달은 건너뛰고 횟수에도 포함하지 않는다. */
    private List<LocalDate> monthly(LocalDate start, int interval, Integer count, LocalDate from, LocalDate last) {
        List<LocalDate> result = new ArrayList<>();
        int dayOfMonth = start.getDayOfMonth();
        YearMonth firstMonth = YearMonth.from(start);
        int produced = 0;

        for (long step = 0; ; step++) {
            YearMonth month = firstMonth.plusMonths(step * interval);
            if (month.atDay(1).isAfter(last)) {
                break;
            }
            if (dayOfMonth > month.lengthOfMonth()) {
                continue;
            }
            if (count != null && produced >= count) {
                break;
            }
            produced++;

            LocalDate date = month.atDay(dayOfMonth);
            if (date.isAfter(last)) {
                break;
            }
            if (!date.isBefore(from)) {
                result.add(date);
            }
        }
        return result;
    }

    private boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
