package com.onebyte.backend.domain.studyrecord.service;

import com.onebyte.backend.domain.studyrecord.dto.StudyRecordPageResponse;
import com.onebyte.backend.domain.studyrecord.dto.StudyRecordPeriod;
import com.onebyte.backend.domain.studyrecord.repository.SessionProblemCount;
import com.onebyte.backend.domain.studyrecord.repository.StudyRecordRepository;
import com.onebyte.backend.domain.studyrecord.repository.StudyRecordRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 날짜 기준은 서버 기본 시간대(KST)이며, 세션은 시작 시각(startedAt) 기준이다.
 * 서버(JVM)의 시간대가 Asia/Seoul이어야 한다. (docker: TZ=Asia/Seoul)
 */
@Service
@RequiredArgsConstructor
public class StudyRecordService {

    private static final int MAX_SIZE = 100;
    /** period=ALL 에서 쓰는 조회 구간 */
    private static final LocalDateTime ALL_FROM = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime ALL_TO = LocalDateTime.of(9999, 1, 1, 0, 0);

    private final StudyRecordRepository studyRecordRepository;

    @Transactional(readOnly = true)
    public StudyRecordPageResponse getRecords(Long userId, StudyRecordPeriod period, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Range range = rangeOf(period, LocalDate.now());

        Page<StudyRecordRow> rows = studyRecordRepository.findRecords(
                userId, range.from(), range.to(), PageRequest.of(safePage, safeSize));

        List<Long> sessionIds = rows.getContent().stream().map(StudyRecordRow::sessionId).toList();
        Map<Long, SessionProblemCount> counts = sessionIds.isEmpty()
                ? Map.of()
                : studyRecordRepository.countProblemResults(sessionIds).stream()
                        .collect(Collectors.toMap(SessionProblemCount::sessionId, Function.identity()));

        List<StudyRecordPageResponse.Item> items = rows.getContent().stream()
                .map(row -> toItem(row, counts.get(row.sessionId())))
                .toList();

        return new StudyRecordPageResponse(
                items, rows.getNumber(), rows.getSize(), rows.getTotalElements(), rows.getTotalPages(), rows.hasNext());
    }

    private StudyRecordPageResponse.Item toItem(StudyRecordRow row, SessionProblemCount count) {
        int solved = count == null ? 0 : count.solvedCount().intValue();
        int correct = count == null ? 0 : count.correctCount().intValue();
        return new StudyRecordPageResponse.Item(
                row.sessionId(),
                row.startedAt(),
                row.recordedMinutes(),
                row.materialId(),
                row.materialName(),
                new StudyRecordPageResponse.ProblemStats(solved, correct, solved - correct),
                row.materialId() != null);
    }

    private Range rangeOf(StudyRecordPeriod period, LocalDate today) {
        return switch (period) {
            case WEEK -> {
                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new Range(weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay());
            }
            case MONTH -> {
                LocalDate monthStart = today.withDayOfMonth(1);
                yield new Range(monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay());
            }
            case ALL -> new Range(ALL_FROM, ALL_TO);
        };
    }

    private record Range(LocalDateTime from, LocalDateTime to) {
    }
}
