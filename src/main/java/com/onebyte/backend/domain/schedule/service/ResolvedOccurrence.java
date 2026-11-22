package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.entity.SchedulePriority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

/**
 * 화면에 표시할 일정 한 회차 (조회 전용).
 * 반복 일정은 scheduleId가 같을 수 있으므로 scheduleId + occurrenceDate로 회차를 식별한다.
 *
 * @param date           화면 표시 날짜 (개별 수정으로 날짜를 옮기면 occurrenceDate와 다를 수 있음)
 * @param occurrenceDate 회차 식별 기준일 (일반 일정은 일정 날짜와 같음)
 * @param time           null이면 종일 일정
 */
public record ResolvedOccurrence(
        Long scheduleId,
        String title,
        LocalDate date,
        LocalDate occurrenceDate,
        LocalTime time,
        LocalDate deadlineDate,
        SchedulePriority priority,
        boolean completed,
        boolean recurring
) {

    /** 날짜 오름차순 → 종일 일정 우선 → 시간 오름차순 (같으면 일정 ID 순) */
    public static final Comparator<ResolvedOccurrence> DISPLAY_ORDER = Comparator
            .comparing(ResolvedOccurrence::date)
            .thenComparing(ResolvedOccurrence::time, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(ResolvedOccurrence::scheduleId)
            .thenComparing(ResolvedOccurrence::occurrenceDate);
}
