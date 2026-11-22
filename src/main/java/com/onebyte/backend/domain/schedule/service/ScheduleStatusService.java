package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.ScheduleCompletionRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCompletionResponse;
import com.onebyte.backend.domain.schedule.entity.ScheduleDeleteScope;
import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
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

/**
 * 일정 완료 / 완료 취소, 일정 삭제.
 * 완료·개별 삭제 상태는 회차(scheduleId + occurrenceDate)별로 schedule_occurrence에 저장한다.
 * 일정 완료는 학습 세션과 무관하며 학습 시간에 영향을 주지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleStatusService {

    private final ScheduleEventRepository scheduleEventRepository;
    private final ScheduleRecurrenceRuleRepository recurrenceRuleRepository;
    private final ScheduleOccurrenceRepository occurrenceRepository;
    private final ScheduleOccurrenceResolver occurrenceResolver;
    private final ScheduleRequestParser requestParser;

    /**
     * 완료 상태를 요청한 값으로 설정한다. (같은 값을 다시 보내도 성공)
     * 일반 일정은 occurrenceDate를 생략하면 일정 날짜, 반복 일정은 occurrenceDate 필수.
     */
    @Transactional
    public ScheduleCompletionResponse changeCompletion(Long userId, Long scheduleId, String occurrenceDateRaw,
                                                       ScheduleCompletionRequest request) {
        if (request == null || request.completed() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "completed 값은 필수입니다.");
        }
        ScheduleEvent schedule = findSchedule(userId, scheduleId);
        ScheduleRecurrenceRule rule = findRule(userId, scheduleId);

        LocalDate occurrenceDate = requestParser.parseOptionalDate(occurrenceDateRaw);
        if (occurrenceDate == null) {
            if (rule != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "반복 일정은 occurrenceDate가 필요합니다.");
            }
            occurrenceDate = schedule.getScheduleDate();
        }
        ScheduleOccurrence state = findExistingOccurrence(userId, schedule, rule, occurrenceDate);

        if (!request.completed() && state == null) {
            // 완료한 적 없는 회차의 완료 취소는 바꿀 상태가 없다
            return new ScheduleCompletionResponse(scheduleId, occurrenceDate, false, schedule.getUpdatedAt());
        }
        ScheduleOccurrence target = (state != null)
                ? state
                : occurrenceRepository.save(ScheduleOccurrence.of(schedule, occurrenceDate));
        if (request.completed()) {
            target.complete(LocalDateTime.now());
        } else {
            target.cancelCompletion();
        }
        occurrenceRepository.flush();
        return new ScheduleCompletionResponse(scheduleId, occurrenceDate, target.isCompleted(), target.getUpdatedAt());
    }

    /**
     * 일정 삭제.
     * - 일반 일정: 범위 없이 일정과 회차 상태를 물리 삭제
     * - 반복 일정 THIS: 선택한 회차만 개별 삭제 상태로 저장 (반복 규칙 유지)
     * - 반복 일정 ALL: 일정·반복 규칙·회차 상태를 모두 물리 삭제
     * 학습 세션 기록은 일정과 연관이 없어 삭제하지 않는다.
     */
    @Transactional
    public void delete(Long userId, Long scheduleId, String scopeRaw, String occurrenceDateRaw) {
        ScheduleEvent schedule = findSchedule(userId, scheduleId);
        ScheduleRecurrenceRule rule = findRule(userId, scheduleId);

        if (rule == null) {
            if (!isBlank(scopeRaw) || !isBlank(occurrenceDateRaw)) {
                throw new CustomException(ErrorCode.INVALID_DELETE_SCOPE,
                        "일반 일정은 삭제 범위(scope, occurrenceDate)를 지정하지 않습니다.");
            }
            deleteSeries(scheduleId, false);
            return;
        }

        switch (parseScope(scopeRaw)) {
            case ALL -> deleteSeries(scheduleId, true);
            case THIS -> {
                if (isBlank(occurrenceDateRaw)) {
                    throw new CustomException(ErrorCode.INVALID_DELETE_SCOPE,
                            "선택한 회차를 삭제하려면 occurrenceDate가 필요합니다.");
                }
                LocalDate occurrenceDate = requestParser.parseOptionalDate(occurrenceDateRaw);
                ScheduleOccurrence state = findExistingOccurrence(userId, schedule, rule, occurrenceDate);
                ScheduleOccurrence target = (state != null)
                        ? state
                        : occurrenceRepository.save(ScheduleOccurrence.of(schedule, occurrenceDate));
                target.markDeleted();
            }
        }
    }

    /** 회차 상태 → 반복 규칙 → 일정 순서로 삭제 (외래키 순서) */
    private void deleteSeries(Long scheduleId, boolean recurring) {
        occurrenceRepository.deleteAllByScheduleId(scheduleId);
        if (recurring) {
            recurrenceRuleRepository.deleteById(scheduleId);
        }
        scheduleEventRepository.deleteById(scheduleId);
    }

    /** 실제 발생일이고 개별 삭제되지 않은 회차인지 확인하고, 저장된 회차 상태(없으면 null)를 돌려준다 */
    private ScheduleOccurrence findExistingOccurrence(Long userId, ScheduleEvent schedule,
                                                     ScheduleRecurrenceRule rule, LocalDate occurrenceDate) {
        ScheduleOccurrence state = occurrenceRepository.findOne(schedule.getId(), occurrenceDate, userId).orElse(null);
        occurrenceResolver.resolveOne(schedule, rule, state, occurrenceDate)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        return state;
    }

    private ScheduleEvent findSchedule(Long userId, Long scheduleId) {
        return scheduleEventRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private ScheduleRecurrenceRule findRule(Long userId, Long scheduleId) {
        return recurrenceRuleRepository.findByScheduleIdAndUserId(scheduleId, userId).orElse(null);
    }

    private ScheduleDeleteScope parseScope(String raw) {
        if (isBlank(raw)) {
            throw new CustomException(ErrorCode.INVALID_DELETE_SCOPE, "반복 일정은 scope(THIS, ALL)가 필요합니다.");
        }
        try {
            return ScheduleDeleteScope.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_DELETE_SCOPE, "scope는 THIS 또는 ALL이어야 합니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
