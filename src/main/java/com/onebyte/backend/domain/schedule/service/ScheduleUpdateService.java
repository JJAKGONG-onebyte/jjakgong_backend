package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.ScheduleUpdateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleUpdateResponse;
import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrenceId;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import com.onebyte.backend.domain.schedule.entity.ScheduleUpdateScope;
import com.onebyte.backend.domain.schedule.repository.ScheduleEventRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleOccurrenceRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleRecurrenceRuleRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 일정 부분 수정 (PATCH).
 *
 * - 일반 일정: 일정 값을 직접 수정. recurrence를 지정하면 반복 일정으로 전환
 * - 반복 일정 THIS: 선택한 회차의 개별 수정 값(schedule_occurrence)만 저장. 반복 설정은 바꿀 수 없음
 * - 반복 일정 ALL_FUTURE: 첫 회차면 시리즈 전체 수정, 아니면 선택한 회차 전날로 기존 시리즈를 끝내고 새 시리즈 생성
 *   영향받는 회차의 개별 수정 값은 초기화하고, 완료·개별 삭제 상태는 새 규칙에서도 발생일인 날짜만 유지한다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleUpdateService {

    private final ScheduleEventRepository scheduleEventRepository;
    private final ScheduleRecurrenceRuleRepository recurrenceRuleRepository;
    private final ScheduleOccurrenceRepository occurrenceRepository;
    private final OccurrenceCalculator occurrenceCalculator;
    private final ScheduleOccurrenceResolver occurrenceResolver;
    private final ScheduleRequestParser requestParser;

    @Transactional
    public ScheduleUpdateResponse update(Long userId, Long scheduleId, String occurrenceDateRaw, String scopeRaw,
                                         ScheduleUpdateRequest request) {
        if (request == null || !request.hasAnyField()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "수정할 항목이 없습니다.");
        }
        ScheduleEvent schedule = scheduleEventRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        ScheduleRecurrenceRule rule = recurrenceRuleRepository.findByScheduleIdAndUserId(scheduleId, userId)
                .orElse(null);

        if (rule == null) {
            if (!isBlank(scopeRaw) || !isBlank(occurrenceDateRaw)) {
                throw new CustomException(ErrorCode.INVALID_UPDATE_SCOPE,
                        "일반 일정은 수정 범위(scope, occurrenceDate)를 지정하지 않습니다.");
            }
            return updateSingle(schedule, request);
        }

        ScheduleUpdateScope scope = parseScope(scopeRaw);
        if (isBlank(occurrenceDateRaw)) {
            throw new CustomException(ErrorCode.INVALID_UPDATE_SCOPE, "반복 일정은 occurrenceDate가 필요합니다.");
        }
        LocalDate occurrenceDate = requestParser.parseOptionalDate(occurrenceDateRaw);
        ScheduleOccurrence state = occurrenceRepository.findOne(scheduleId, occurrenceDate, userId).orElse(null);
        ResolvedOccurrence current = occurrenceResolver.resolveOne(schedule, rule, state, occurrenceDate)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));

        return switch (scope) {
            case THIS -> updateThis(schedule, rule, state, current, request);
            case ALL_FUTURE -> updateAllFuture(schedule, rule, occurrenceDate, request);
        };
    }

    // ===== 일반 일정 =====

    private ScheduleUpdateResponse updateSingle(ScheduleEvent schedule, ScheduleUpdateRequest request) {
        LocalDate oldDate = schedule.getScheduleDate();
        Values values = merge(new Values(schedule.getTitle(), oldDate, schedule.getScheduleTime(),
                schedule.getDeadlineDate(), schedule.getPriority()), request);

        ScheduleRecurrenceRule newRule = null;
        if (request.hasRecurrence() && request.recurrence() != null) {
            ScheduleCreateCommand.Recurrence recurrence = requestParser.parseRecurrence(request.recurrence(), values.date());
            newRule = recurrenceRuleRepository.save(ScheduleRecurrenceRule.builder()
                    .schedule(schedule)
                    .frequency(recurrence.frequency())
                    .repeatEndDate(recurrence.endDate())
                    .repeatCount(recurrence.count())
                    .build());
        }
        schedule.update(values.title(), values.date(), values.time(), values.deadlineDate(), values.priority());

        List<ScheduleOccurrence> rows = occurrenceRepository
                .findById(new ScheduleOccurrenceId(schedule.getId(), oldDate))
                .stream().toList();
        carryStates(rows, oldDate, schedule, values.date(), newRule, true);
        return respond(schedule, newRule, values.date());
    }

    // ===== 반복 일정: 선택한 회차만 =====

    private ScheduleUpdateResponse updateThis(ScheduleEvent schedule, ScheduleRecurrenceRule rule,
                                              ScheduleOccurrence state, ResolvedOccurrence current,
                                              ScheduleUpdateRequest request) {
        if (request.hasRecurrence()) {
            throw new CustomException(ErrorCode.INVALID_UPDATE_SCOPE, "선택한 회차만 수정할 때는 반복 설정을 변경할 수 없습니다.");
        }
        Values values = merge(new Values(current.title(), current.date(), current.time(),
                current.deadlineDate(), current.priority()), request);

        ScheduleOccurrence target = (state != null)
                ? state
                : occurrenceRepository.save(ScheduleOccurrence.of(schedule, current.occurrenceDate()));

        if (request.hasTitle()) {
            target.overrideTitle(values.title());
        }
        if (request.hasDate()) {
            target.overrideDate(values.date().equals(current.occurrenceDate()) ? null : values.date());
        }
        if (request.hasTime()) {
            if (values.time() == null) {
                target.overrideAllDay();
            } else {
                target.overrideTime(values.time());
            }
        }
        if (request.hasDeadlineDate()) {
            if (values.deadlineDate() == null) {
                target.clearDeadline();
            } else {
                target.overrideDeadline(values.deadlineDate());
            }
        }
        if (request.hasPriority()) {
            target.overridePriority(values.priority());
        }

        occurrenceRepository.flush();
        ResolvedOccurrence result = occurrenceResolver.resolveOne(schedule, rule, target, current.occurrenceDate())
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        return ScheduleUpdateResponse.of(result, rule, target.getUpdatedAt());
    }

    // ===== 반복 일정: 선택한 회차부터 이후 전체 =====

    private ScheduleUpdateResponse updateAllFuture(ScheduleEvent schedule, ScheduleRecurrenceRule rule,
                                                   LocalDate occurrenceDate, ScheduleUpdateRequest request) {
        LocalDate start = schedule.getScheduleDate();
        LocalDate baseDeadline = (schedule.getDeadlineDate() == null)
                ? null
                : occurrenceDate.plusDays(ChronoUnit.DAYS.between(start, schedule.getDeadlineDate()));
        Values values = merge(new Values(schedule.getTitle(), occurrenceDate, schedule.getScheduleTime(),
                baseDeadline, schedule.getPriority()), request);

        ScheduleCreateCommand.Recurrence recurrence = nextRecurrence(start, rule, occurrenceDate, values.date(), request);
        List<ScheduleOccurrence> rows = occurrenceRepository.findAllFrom(schedule.getId(), occurrenceDate);

        if (occurrenceDate.equals(start)) {
            // 첫 회차부터 수정: 시리즈 전체를 그대로 수정
            schedule.update(values.title(), values.date(), values.time(), values.deadlineDate(), values.priority());
            ScheduleRecurrenceRule targetRule = rule;
            if (recurrence == null) {
                recurrenceRuleRepository.delete(rule);
                targetRule = null;
            } else if (request.hasRecurrence()) {
                rule.change(recurrence.frequency(), recurrence.endDate(), recurrence.count());
            }
            carryStates(rows, occurrenceDate, schedule, values.date(), targetRule, true);
            return respond(schedule, targetRule, values.date());
        }

        // 중간 회차부터 수정: 기존 시리즈는 전날까지, 새 시리즈를 선택한 회차부터
        rule.endAt(occurrenceDate.minusDays(1));
        ScheduleEvent newSchedule = scheduleEventRepository.save(ScheduleEvent.builder()
                .user(schedule.getUser())
                .title(values.title())
                .eventType(schedule.getEventType())
                .scheduleDate(values.date())
                .scheduleTime(values.time())
                .deadlineDate(values.deadlineDate())
                .priority(values.priority())
                .build());
        ScheduleRecurrenceRule newRule = null;
        if (recurrence != null) {
            newRule = recurrenceRuleRepository.save(ScheduleRecurrenceRule.builder()
                    .schedule(newSchedule)
                    .frequency(recurrence.frequency())
                    .repeatEndDate(recurrence.endDate())
                    .repeatCount(recurrence.count())
                    .build());
        }
        carryStates(rows, occurrenceDate, newSchedule, values.date(), newRule, false);
        return respond(newSchedule, newRule, values.date());
    }

    /**
     * 수정 후 적용할 반복 설정. null이면 반복 해제.
     * recurrence를 보내지 않으면 기존 주기·종료일을 유지하고, 횟수 기준이면 남은 횟수로 계산한다.
     */
    private ScheduleCreateCommand.Recurrence nextRecurrence(LocalDate start, ScheduleRecurrenceRule rule,
                                                            LocalDate occurrenceDate, LocalDate newStart,
                                                            ScheduleUpdateRequest request) {
        if (request.hasRecurrence()) {
            return (request.recurrence() == null)
                    ? null
                    : requestParser.parseRecurrence(request.recurrence(), newStart);
        }
        if (rule.getRepeatEndDate() != null && rule.getRepeatEndDate().isBefore(newStart)) {
            throw new CustomException(ErrorCode.INVALID_RECURRENCE, "반복 종료일은 일정 시작일보다 빠를 수 없습니다.");
        }
        Integer remaining = null;
        if (rule.getRepeatCount() != null) {
            int before = occurrenceCalculator.occurrencesBetween(start, rule, start, occurrenceDate.minusDays(1)).size();
            remaining = rule.getRepeatCount() - before;
        }
        return new ScheduleCreateCommand.Recurrence(rule.getFrequency(), rule.getRepeatEndDate(), remaining);
    }

    // ===== 회차 상태 이전 =====

    /**
     * 회차 상태(완료·개별 삭제)를 수정 결과에 맞게 옮긴다.
     * - 선택한 회차의 상태는 새 시작일(targetStart)로 옮긴다.
     * - 나머지 회차는 같은 날짜가 새 규칙의 발생일일 때만 상태를 유지하고, 개별 수정 값은 초기화한다.
     *
     * @param sameSchedule true면 같은 일정 안에서 정리, false면 target(새 시리즈)으로 옮긴다
     */
    private void carryStates(List<ScheduleOccurrence> rows, LocalDate selectedDate, ScheduleEvent target,
                             LocalDate targetStart, ScheduleRecurrenceRule targetRule, boolean sameSchedule) {
        List<ScheduleOccurrence> toDelete = new ArrayList<>();
        List<ScheduleOccurrence> moving = new ArrayList<>();
        Set<LocalDate> occupied = new HashSet<>();

        for (ScheduleOccurrence row : rows) {
            LocalDate from = row.getId().getOccurrenceDate();
            boolean selected = from.equals(selectedDate);
            LocalDate to = selected ? targetStart : from;

            if (sameSchedule && to.equals(from)) {
                // 선택한 회차가 옮겨 올 자리의 기존 상태는 선택한 회차의 상태로 대체한다
                boolean replacedBySelected = !selected && from.equals(targetStart) && !selectedDate.equals(targetStart);
                if (!replacedBySelected && row.hasStatus() && isOccurrence(targetStart, targetRule, to)) {
                    row.resetOverrides();
                    occupied.add(from);
                } else {
                    toDelete.add(row);
                }
            } else {
                moving.add(row);
                toDelete.add(row);
            }
        }

        List<ScheduleOccurrence> toCreate = new ArrayList<>();
        for (ScheduleOccurrence row : moving) {
            LocalDate from = row.getId().getOccurrenceDate();
            LocalDate to = from.equals(selectedDate) ? targetStart : from;
            if (row.hasStatus() && isOccurrence(targetStart, targetRule, to) && occupied.add(to)) {
                ScheduleOccurrence created = ScheduleOccurrence.of(target, to);
                created.copyStatusFrom(row);
                toCreate.add(created);
            }
        }

        // Hibernate는 flush 시 insert를 delete보다 먼저 실행하므로, 같은 키 충돌을 막기 위해 삭제를 먼저 반영한다
        occurrenceRepository.deleteAll(toDelete);
        occurrenceRepository.flush();
        occurrenceRepository.saveAll(toCreate);
    }

    private boolean isOccurrence(LocalDate start, ScheduleRecurrenceRule rule, LocalDate date) {
        return occurrenceCalculator.isOccurrence(start, rule, date);
    }

    // ===== 공통 =====

    private ScheduleUpdateResponse respond(ScheduleEvent schedule, ScheduleRecurrenceRule rule, LocalDate occurrenceDate) {
        scheduleEventRepository.flush();
        ScheduleOccurrence state = occurrenceRepository
                .findById(new ScheduleOccurrenceId(schedule.getId(), occurrenceDate))
                .orElse(null);
        ResolvedOccurrence result = occurrenceResolver.resolveOne(schedule, rule, state, occurrenceDate)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        return ScheduleUpdateResponse.of(result, rule, latest(schedule.getUpdatedAt(),
                rule == null ? null : rule.getUpdatedAt()));
    }

    /** 요청에 있는 필드만 기존 값에 덮어쓰고, 등록과 같은 규칙으로 검증한다 */
    private Values merge(Values base, ScheduleUpdateRequest request) {
        String title = request.hasTitle() ? requestParser.parseTitle(request.title()) : base.title();
        LocalDate date = request.hasDate() ? requestParser.parseRequiredDate(request.date(), "date") : base.date();
        LocalTime time = request.hasTime() ? requestParser.parseTime(request.time()) : base.time();
        LocalDate deadlineDate = request.hasDeadlineDate()
                ? requestParser.parseOptionalDate(request.deadlineDate())
                : base.deadlineDate();

        SchedulePriority priority = base.priority();
        if (request.hasPriority()) {
            if (isBlank(request.priority())) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "우선순위는 HIGH, MEDIUM, LOW 중 하나여야 합니다.");
            }
            priority = requestParser.parsePriority(request.priority());
        }

        if (deadlineDate != null && deadlineDate.isBefore(date)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "마감일은 일정 날짜보다 빠를 수 없습니다.");
        }
        return new Values(title, date, time, deadlineDate, priority);
    }

    private ScheduleUpdateScope parseScope(String raw) {
        if (isBlank(raw)) {
            throw new CustomException(ErrorCode.INVALID_UPDATE_SCOPE, "반복 일정은 scope(THIS, ALL_FUTURE)가 필요합니다.");
        }
        try {
            return ScheduleUpdateScope.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_UPDATE_SCOPE, "scope는 THIS 또는 ALL_FUTURE여야 합니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static LocalDateTime latest(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private record Values(String title, LocalDate date, LocalTime time, LocalDate deadlineDate,
                          SchedulePriority priority) {
    }
}
