package com.onebyte.backend.domain.studysession.service;

import com.onebyte.backend.domain.studysession.entity.SessionEndType;
import com.onebyte.backend.domain.studysession.entity.StudyBreak;
import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.studysession.repository.StudyBreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 세션 종료 공통 처리 (정상 종료 / 비정상 종료 자동 처리).
 * 진행 중인 휴식을 함께 끝내고, 휴식 시간을 뺀 학습 시간을 계산한다.
 */
@Component
@RequiredArgsConstructor
public class StudySessionCloser {

    /** 마지막 활동 후 이 시간이 지나면 비정상 종료로 본다 */
    public static final Duration INACTIVE_LIMIT = Duration.ofMinutes(10);

    private final StudyBreakRepository studyBreakRepository;

    /** 종료 버튼으로 정상 종료 */
    public void closeByUser(StudySession session, LocalDateTime now) {
        close(session, now, SessionEndType.USER);
    }

    /** 비정상 종료: 마지막 활동 시각까지를 학습 시간으로 기록 */
    public void closeAsInactive(StudySession session) {
        close(session, session.getLastActivityAt(), SessionEndType.AUTO);
    }

    public boolean isInactive(StudySession session, LocalDateTime now) {
        return session.isInactiveSince(now.minus(INACTIVE_LIMIT));
    }

    private void close(StudySession session, LocalDateTime endedAt, SessionEndType endType) {
        List<StudyBreak> breaks = studyBreakRepository.findAllBySession_Id(session.getId());

        long breakSeconds = 0;
        for (StudyBreak studyBreak : breaks) {
            breakSeconds += studyBreak.durationSecondsUntil(endedAt);
            if (studyBreak.isOngoing()) {
                studyBreak.end(endedAt);
            }
        }

        long totalSeconds = Duration.between(session.getStartedAt(), endedAt).getSeconds();
        int recordedMinutes = (int) Math.max(0, (totalSeconds - breakSeconds) / 60);

        session.end(endedAt, endType, recordedMinutes);
    }
}
