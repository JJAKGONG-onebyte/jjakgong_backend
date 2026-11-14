package com.onebyte.backend.domain.studystats.service;

import com.onebyte.backend.domain.studystats.dto.StreakResponse;
import com.onebyte.backend.domain.studystats.dto.WeeklyStatsResponse;
import com.onebyte.backend.domain.studystats.repository.SessionMinutes;
import com.onebyte.backend.domain.studystats.repository.StudyStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 날짜 기준은 서버 기본 시간대(KST)이며, 세션은 시작 시각(startedAt)의 날짜에 귀속한다.
 * 서버(JVM)의 시간대가 Asia/Seoul이어야 한다. (docker: TZ=Asia/Seoul)
 */
@Service
@RequiredArgsConstructor
public class StudyStatsService {

    /** 하루 합계가 이 값(분) 이상이면 학습일로 인정 */
    private static final int MIN_MINUTES_PER_STUDY_DAY = 10;
    /** 연속 학습일 계산 시 거슬러 올라갈 최대 일수 */
    private static final int STREAK_LOOKBACK_DAYS = 365;

    private final StudyStatsRepository studyStatsRepository;

    /** 이번 주(월~일) 요일별 학습 시간. 공부하지 않은 날과 아직 오지 않은 요일은 0분 */
    @Transactional(readOnly = true)
    public WeeklyStatsResponse getWeekly(Long userId) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, Integer> minutesByDate = sumByDate(userId, weekStart, weekStart.plusDays(7));

        List<WeeklyStatsResponse.Day> days = IntStream.range(0, 7)
                .mapToObj(weekStart::plusDays)
                .map(date -> new WeeklyStatsResponse.Day(
                        date,
                        date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT),
                        minutesByDate.getOrDefault(date, 0)))
                .toList();

        int total = days.stream().mapToInt(WeeklyStatsResponse.Day::minutes).sum();
        return new WeeklyStatsResponse(weekStart, total, days);
    }

    /**
     * 연속 학습일. 오늘 이미 학습일이면 오늘부터, 아직이면 어제부터 거꾸로 세어
     * 오늘 아직 공부하지 않았다고 기록이 0으로 보이지 않게 한다.
     */
    @Transactional(readOnly = true)
    public StreakResponse getStreak(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate oldest = today.minusDays(STREAK_LOOKBACK_DAYS);
        Map<LocalDate, Integer> minutesByDate = sumByDate(userId, oldest, today.plusDays(1));

        boolean studiedToday = isStudyDay(minutesByDate, today);
        LocalDate cursor = studiedToday ? today : today.minusDays(1);

        int streak = 0;
        while (!cursor.isBefore(oldest) && isStudyDay(minutesByDate, cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return new StreakResponse(streak, studiedToday);
    }

    /** [fromDate, toDate) 구간의 날짜별 학습 시간(분) 합계 */
    private Map<LocalDate, Integer> sumByDate(Long userId, LocalDate fromDate, LocalDate toDate) {
        List<SessionMinutes> sessions = studyStatsRepository.findFinishedSessionMinutes(
                userId, fromDate.atStartOfDay(), toDate.atStartOfDay());
        return sessions.stream().collect(Collectors.groupingBy(
                s -> s.startedAt().toLocalDate(),
                Collectors.summingInt(SessionMinutes::recordedMinutes)));
    }

    private boolean isStudyDay(Map<LocalDate, Integer> minutesByDate, LocalDate date) {
        return minutesByDate.getOrDefault(date, 0) >= MIN_MINUTES_PER_STUDY_DAY;
    }
}
