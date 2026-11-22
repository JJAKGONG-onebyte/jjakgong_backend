package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleDetailResponse;
import com.onebyte.backend.domain.schedule.dto.ScheduleListResponse;
import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import com.onebyte.backend.domain.schedule.repository.ScheduleEventRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleOccurrenceRepository;
import com.onebyte.backend.domain.schedule.repository.ScheduleRecurrenceRuleRepository;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    /** 한 번에 조회할 수 있는 최대 기간(일). 종료 조건 없는 반복 일정이 과도하게 펼쳐지는 것을 막는다. */
    static final int MAX_RANGE_DAYS = 366;

    private final ScheduleEventRepository scheduleEventRepository;
    private final ScheduleRecurrenceRuleRepository recurrenceRuleRepository;
    private final ScheduleOccurrenceRepository occurrenceRepository;
    private final UserRepository userRepository;
    private final ScheduleOccurrenceResolver occurrenceResolver;
    private final ScheduleRequestParser requestParser;

    /** 일정 등록. 반복 일정이면 반복 규칙을 같은 트랜잭션에서 저장한다. */
    @Transactional
    public ScheduleCreateResponse create(Long userId, ScheduleCreateRequest request) {
        ScheduleCreateCommand command = requestParser.parseCreate(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ScheduleEvent schedule = scheduleEventRepository.save(ScheduleEvent.builder()
                .user(user)
                .title(command.title())
                .scheduleDate(command.date())
                .scheduleTime(command.time())
                .deadlineDate(command.deadlineDate())
                .priority(command.priority())
                .build());

        ScheduleRecurrenceRule rule = null;
        if (command.recurrence() != null) {
            rule = recurrenceRuleRepository.save(ScheduleRecurrenceRule.builder()
                    .schedule(schedule)
                    .frequency(command.recurrence().frequency())
                    .repeatEndDate(command.recurrence().endDate())
                    .repeatCount(command.recurrence().count())
                    .build());
        }
        return ScheduleCreateResponse.of(schedule, rule);
    }

    /** 일정 목록 / 캘린더 조회 (반복 일정은 발생일별) */
    @Transactional(readOnly = true)
    public ScheduleListResponse getList(Long userId, String startDateRaw, String endDateRaw) {
        LocalDate startDate = requestParser.parseRequiredDate(startDateRaw, "startDate");
        LocalDate endDate = requestParser.parseRequiredDate(endDateRaw, "endDate");
        validateRange(startDate, endDate);
        return ScheduleListResponse.of(startDate, endDate, findOccurrences(userId, startDate, endDate));
    }

    /**
     * [startDate, endDate] 구간에 표시할 본인 일정 회차 (표시 순서로 정렬).
     * 대시보드의 오늘 일정 등 다른 기능에서도 재사용한다.
     */
    @Transactional(readOnly = true)
    public List<ResolvedOccurrence> findOccurrences(Long userId, LocalDate startDate, LocalDate endDate) {
        List<ScheduleEvent> schedules = scheduleEventRepository.findCandidatesInRange(userId, startDate, endDate);
        if (schedules.isEmpty()) {
            return List.of();
        }
        List<Long> scheduleIds = schedules.stream().map(ScheduleEvent::getId).toList();

        Map<Long, ScheduleRecurrenceRule> rules = recurrenceRuleRepository
                .findAllByScheduleIdsAndUserId(scheduleIds, userId).stream()
                .collect(Collectors.toMap(ScheduleRecurrenceRule::getScheduleId, Function.identity()));
        List<ScheduleOccurrence> occurrences = occurrenceRepository
                .findAllInRange(scheduleIds, userId, startDate, endDate);

        return occurrenceResolver.resolve(schedules, rules, occurrences, startDate, endDate);
    }

    /**
     * 일정 상세 조회.
     * 반복 일정은 occurrenceDate가 필수이며, 일반 일정은 생략하면 일정 날짜로 조회한다.
     * 없는 일정·남의 일정·실제 발생일이 아닌 날짜·개별 삭제된 회차는 모두 404.
     */
    @Transactional(readOnly = true)
    public ScheduleDetailResponse getDetail(Long userId, Long scheduleId, String occurrenceDateRaw) {
        ScheduleEvent schedule = scheduleEventRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        ScheduleRecurrenceRule rule = recurrenceRuleRepository.findByScheduleIdAndUserId(scheduleId, userId)
                .orElse(null);

        LocalDate occurrenceDate = requestParser.parseOptionalDate(occurrenceDateRaw);
        if (occurrenceDate == null) {
            if (rule != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "반복 일정은 occurrenceDate가 필요합니다.");
            }
            occurrenceDate = schedule.getScheduleDate();
        }

        ScheduleOccurrence state = occurrenceRepository.findOne(scheduleId, occurrenceDate, userId).orElse(null);
        ResolvedOccurrence occurrence = occurrenceResolver.resolveOne(schedule, rule, state, occurrenceDate)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        return ScheduleDetailResponse.of(occurrence, schedule, rule);
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_RANGE_DAYS) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE, "조회 기간은 최대 " + MAX_RANGE_DAYS + "일입니다.");
        }
    }
}
