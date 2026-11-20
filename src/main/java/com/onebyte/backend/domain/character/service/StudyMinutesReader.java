package com.onebyte.backend.domain.character.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 레벨 계산용 누적 학습 시간 조회.
 * 학습 세션은 다른 도메인이라 Repository를 건드리지 않고 EntityManager로 조회만 한다.
 */
@Component
@RequiredArgsConstructor
public class StudyMinutesReader {

    private final EntityManager em;

    /** 종료된 세션의 기록 학습 시간(분) 합계 */
    public long readTotalMinutes(Long userId) {
        Long total = em.createQuery(
                        "select sum(s.recordedMinutes) from StudySession s"
                                + " where s.user.id = :userId and s.endedAt is not null"
                                + " and s.recordedMinutes is not null",
                        Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return total == null ? 0L : total;
    }
}