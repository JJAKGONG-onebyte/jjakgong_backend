package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrenceId;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 일정 + 반복 규칙 + 회차 상태(완료, 개별 수정, 개별 삭제)를 합쳐 화면용 회차 목록을 만든다.
 * 조회는 하지 않으며, 호출하는 쪽에서 본인 일정만 조회해 넘겨야 한다.
 *
 * - 개별 삭제된 회차는 제외한다.
 * - 개별 수정(override*)은 해당 회차에만 반영하고, 날짜를 옮겨도 occurrenceDate는 유지한다.
 * - 반복 일정의 회차별 마감일 = 회차일 + (원본 마감일 − 원본 일정 날짜)
 */
@Component
@RequiredArgsConstructor
public class ScheduleOccurrenceResolver {

    private final OccurrenceCalculator occurrenceCalculator;

    /**
     * [from, to] 구간에 표시할 회차 목록 (표시 순서로 정렬).
     *
     * @param rulesByScheduleId 반복 일정의 규칙 (일반 일정은 없음)
     * @param occurrences       구간과 관련된 회차 상태 (발생일 또는 옮긴 날짜가 구간 안)
     */
    public List<ResolvedOccurrence> resolve(Collection<ScheduleEvent> schedules,
                                            Map<Long, ScheduleRecurrenceRule> rulesByScheduleId,
                                            Collection<ScheduleOccurrence> occurrences,
                                            LocalDate from, LocalDate to) {
        Map<ScheduleOccurrenceId, ScheduleOccurrence> stateById = occurrences.stream()
                .collect(Collectors.toMap(ScheduleOccurrence::getId, Function.identity(), (a, b) -> a));

        List<ResolvedOccurrence> result = new ArrayList<>();
        for (ScheduleEvent schedule : schedules) {
            ScheduleRecurrenceRule rule = rulesByScheduleId.get(schedule.getId());

            for (LocalDate occurrenceDate : candidateDates(schedule, rule, stateById.values(), from, to)) {
                ScheduleOccurrence state = stateById.get(new ScheduleOccurrenceId(schedule.getId(), occurrenceDate));
                toResolved(schedule, rule, state, occurrenceDate)
                        .filter(o -> isWithin(o.date(), from, to))
                        .ifPresent(result::add);
            }
        }
        result.sort(ResolvedOccurrence.DISPLAY_ORDER);
        return result;
    }

    /**
     * 특정 회차 하나 (상세 조회 등).
     * 실제 발생일이 아니거나 개별 삭제된 회차면 빈 값.
     *
     * @param rule  반복 규칙 (일반 일정이면 null)
     * @param state 해당 회차 상태 (기록이 없으면 null)
     */
    public Optional<ResolvedOccurrence> resolveOne(ScheduleEvent schedule, ScheduleRecurrenceRule rule,
                                                   ScheduleOccurrence state, LocalDate occurrenceDate) {
        if (!occurrenceCalculator.isOccurrence(schedule.getScheduleDate(), rule, occurrenceDate)) {
            return Optional.empty();
        }
        return toResolved(schedule, rule, state, occurrenceDate);
    }

    /** 구간 안의 발생일 + 다른 날짜에서 구간 안으로 옮겨 온 회차의 발생일 */
    private TreeSet<LocalDate> candidateDates(ScheduleEvent schedule, ScheduleRecurrenceRule rule,
                                              Collection<ScheduleOccurrence> states, LocalDate from, LocalDate to) {
        TreeSet<LocalDate> dates = new TreeSet<>(
                occurrenceCalculator.occurrencesBetween(schedule.getScheduleDate(), rule, from, to));

        for (ScheduleOccurrence state : states) {
            LocalDate occurrenceDate = state.getId().getOccurrenceDate();
            if (state.getId().getScheduleId().equals(schedule.getId())
                    && state.getOverrideDate() != null
                    && isWithin(state.getOverrideDate(), from, to)
                    && !dates.contains(occurrenceDate)
                    && occurrenceCalculator.isOccurrence(schedule.getScheduleDate(), rule, occurrenceDate)) {
                dates.add(occurrenceDate);
            }
        }
        return dates;
    }

    private Optional<ResolvedOccurrence> toResolved(ScheduleEvent schedule, ScheduleRecurrenceRule rule,
                                                    ScheduleOccurrence state, LocalDate occurrenceDate) {
        if (state != null && state.isDeleted()) {
            return Optional.empty();
        }

        String title = schedule.getTitle();
        LocalDate date = occurrenceDate;
        LocalTime time = schedule.getScheduleTime();
        LocalDate deadlineDate = baseDeadline(schedule, rule, occurrenceDate);
        var priority = schedule.getPriority();
        boolean completed = false;

        if (state != null) {
            if (state.getOverrideTitle() != null) {
                title = state.getOverrideTitle();
            }
            if (state.getOverrideDate() != null) {
                date = state.getOverrideDate();
            }
            if (state.isOverrideAllDay()) {
                time = null;
            } else if (state.getOverrideTime() != null) {
                time = state.getOverrideTime();
            }
            if (state.isClearDeadline()) {
                deadlineDate = null;
            } else if (state.getOverrideDeadlineDate() != null) {
                deadlineDate = state.getOverrideDeadlineDate();
            }
            if (state.getOverridePriority() != null) {
                priority = state.getOverridePriority();
            }
            completed = state.isCompleted();
        }

        return Optional.of(new ResolvedOccurrence(
                schedule.getId(), title, date, occurrenceDate, time, deadlineDate, priority, completed, rule != null));
    }

    /** 일반 일정은 저장된 마감일, 반복 일정은 원본 일정과 마감일의 간격을 회차일에 더한 날짜 */
    private LocalDate baseDeadline(ScheduleEvent schedule, ScheduleRecurrenceRule rule, LocalDate occurrenceDate) {
        if (schedule.getDeadlineDate() == null) {
            return null;
        }
        if (rule == null) {
            return schedule.getDeadlineDate();
        }
        long offsetDays = ChronoUnit.DAYS.between(schedule.getScheduleDate(), schedule.getDeadlineDate());
        return occurrenceDate.plusDays(offsetDays);
    }

    private boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
