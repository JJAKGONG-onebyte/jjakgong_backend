package com.onebyte.backend.domain.schedule.service;

import com.onebyte.backend.domain.schedule.dto.RecurrenceRequest;
import com.onebyte.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.onebyte.backend.domain.schedule.entity.RecurrenceFrequency;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * 일정 요청 값(문자열) 변환 및 검증.
 * 형식 오류를 전역 핸들러의 INVALID_INPUT이 아니라 명세서의 에러 코드로 응답하기 위해 직접 파싱한다.
 */
@Component
public class ScheduleRequestParser {

    static final int TITLE_MAX_LENGTH = 100;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    public ScheduleCreateCommand parseCreate(ScheduleCreateRequest request) {
        String title = parseTitle(request.title());
        LocalDate date = parseRequiredDate(request.date(), "date");
        LocalTime time = parseTime(request.time());
        LocalDate deadlineDate = parseDeadline(request.deadlineDate(), date);
        SchedulePriority priority = parsePriority(request.priority());
        ScheduleCreateCommand.Recurrence recurrence = parseRecurrence(request.recurrence(), date);
        return new ScheduleCreateCommand(title, date, time, deadlineDate, priority, recurrence);
    }

    /** 앞뒤 공백 제거. 누락·공백만 입력하면 TITLE_REQUIRED */
    public String parseTitle(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CustomException(ErrorCode.TITLE_REQUIRED);
        }
        String title = raw.trim();
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "일정 제목은 " + TITLE_MAX_LENGTH + "자 이하여야 합니다.");
        }
        return title;
    }

    /** 필수 날짜 (yyyy-MM-dd) */
    public LocalDate parseRequiredDate(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, fieldName + "는 필수 입력 항목입니다.");
        }
        return parseDate(raw);
    }

    /** 선택 날짜 (yyyy-MM-dd). 누락·빈 값이면 null */
    public LocalDate parseOptionalDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseDate(raw);
    }

    /** 시간 (HH:mm). 누락·빈 값이면 null(종일 일정) */
    public LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim(), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_DATE_FORMAT, "시간 형식은 HH:mm이어야 합니다.");
        }
    }

    /** 마감일은 일정 날짜보다 빠를 수 없다 */
    public LocalDate parseDeadline(String raw, LocalDate date) {
        LocalDate deadlineDate = parseOptionalDate(raw);
        if (deadlineDate != null && deadlineDate.isBefore(date)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "마감일은 일정 날짜보다 빠를 수 없습니다.");
        }
        return deadlineDate;
    }

    /** 누락 시 MEDIUM */
    public SchedulePriority parsePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return SchedulePriority.MEDIUM;
        }
        try {
            return SchedulePriority.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "우선순위는 HIGH, MEDIUM, LOW 중 하나여야 합니다.");
        }
    }

    /** 반복 설정. request가 null이면 일반 일정(null 반환) */
    public ScheduleCreateCommand.Recurrence parseRecurrence(RecurrenceRequest request, LocalDate startDate) {
        if (request == null) {
            return null;
        }
        RecurrenceFrequency frequency = parseFrequency(request.frequency());
        LocalDate endDate = parseOptionalDate(request.endDate());
        Integer count = request.count();

        if (endDate != null && count != null) {
            throw new CustomException(ErrorCode.INVALID_RECURRENCE, "반복 종료일과 반복 횟수는 함께 지정할 수 없습니다.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.INVALID_RECURRENCE, "반복 종료일은 일정 시작일보다 빠를 수 없습니다.");
        }
        if (count != null && count < 1) {
            throw new CustomException(ErrorCode.INVALID_RECURRENCE, "반복 횟수는 1 이상이어야 합니다.");
        }
        return new ScheduleCreateCommand.Recurrence(frequency, endDate, count);
    }

    private RecurrenceFrequency parseFrequency(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_RECURRENCE, "반복 주기(frequency)는 필수 입력 항목입니다.");
        }
        try {
            return RecurrenceFrequency.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_RECURRENCE, "반복 주기는 DAILY, WEEKLY, MONTHLY 중 하나여야 합니다.");
        }
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
        }
    }
}
