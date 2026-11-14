
package com.onebyte.backend.domain.studysession.repository;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    /** 본인 세션만 조회 (남의 세션이면 빈 값) */
    Optional<StudySession> findByIdAndUser_Id(Long sessionId, Long userId);

    /** 사용자의 진행 중(종료되지 않은) 세션 */
    Optional<StudySession> findFirstByUser_IdAndEndedAtIsNullOrderByStartedAtDesc(Long userId);

    /** 종료되지 않았고 threshold 이전부터 활동이 없는 세션 ID (스케줄러용) */
    @Query("select s.id from StudySession s where s.endedAt is null and s.lastActivityAt < :threshold")
    List<Long> findInactiveSessionIds(@Param("threshold") LocalDateTime threshold);
}
