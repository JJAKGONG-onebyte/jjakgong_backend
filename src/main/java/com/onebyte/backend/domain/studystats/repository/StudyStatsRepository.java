package com.onebyte.backend.domain.studystats.repository;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 학습 통계 전용 조회 레포지토리.
 * StudySessionRepository(학습화면 담당)와 파일이 겹치지 않도록 별도 인터페이스로 분리했다.
 */
public interface StudyStatsRepository extends Repository<StudySession, Long> {

    /** [from, to) 구간에 시작한, 종료된 세션의 학습 시간 */
    @Query("""
            select new com.onebyte.backend.domain.studystats.repository.SessionMinutes(
                s.startedAt, s.recordedMinutes)
            from StudySession s
            where s.user.id = :userId
              and s.endedAt is not null
              and s.recordedMinutes is not null
              and s.startedAt >= :from
              and s.startedAt < :to
            """)
    List<SessionMinutes> findFinishedSessionMinutes(@Param("userId") Long userId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
