package com.onebyte.backend.domain.studysession.service;

import com.onebyte.backend.domain.focus.entity.FocusEventType;
import com.onebyte.backend.domain.studysession.dto.EndSessionResponse.FocusEventStats;
import com.onebyte.backend.domain.studysession.dto.EndSessionResponse.ProblemStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 학습 종료 화면에 보여줄 세션 통계 조회.
 * 문제·집중 이벤트는 다른 도메인이라 Repository 대신 EntityManager로 조회만 한다.
 */
@Component
@RequiredArgsConstructor
public class StudySessionStatsReader {

    private final EntityManager em;

    public ProblemStats readProblemStats(Long sessionId) {
        Long solved = em.createQuery(
                        "select count(ps) from ProblemSubmission ps where ps.problem.session.id = :sessionId",
                        Long.class)
                .setParameter("sessionId", sessionId)
                .getSingleResult();
        Long correct = em.createQuery(
                        "select count(ps) from ProblemSubmission ps"
                                + " where ps.problem.session.id = :sessionId and ps.correct = true",
                        Long.class)
                .setParameter("sessionId", sessionId)
                .getSingleResult();
        return new ProblemStats(solved, correct);
    }

    public FocusEventStats readFocusEventStats(Long sessionId) {
        List<Object[]> rows = em.createQuery(
                        "select fe.eventType, count(fe) from FocusEvent fe"
                                + " where fe.session.id = :sessionId and fe.falsePositive = false"
                                + " group by fe.eventType",
                        Object[].class)
                .setParameter("sessionId", sessionId)
                .getResultList();

        Map<FocusEventType, Long> counts = new EnumMap<>(FocusEventType.class);
        for (Object[] row : rows) {
            counts.put((FocusEventType) row[0], (Long) row[1]);
        }
        Long falsePositiveCount = em.createQuery(
                        "select count(fe) from FocusEvent fe"
                                + " where fe.session.id = :sessionId and fe.falsePositive = true",
                        Long.class)
                .setParameter("sessionId", sessionId)
                .getSingleResult();

        return new FocusEventStats(
                counts.getOrDefault(FocusEventType.AWAY, 0L),
                counts.getOrDefault(FocusEventType.GAZE_OFF, 0L),
                counts.getOrDefault(FocusEventType.DROWSY, 0L),
                falsePositiveCount
        );
    }
}
