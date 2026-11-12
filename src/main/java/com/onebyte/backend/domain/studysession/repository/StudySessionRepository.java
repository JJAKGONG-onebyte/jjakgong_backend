
package com.onebyte.backend.domain.studysession.repository;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    /** 본인 세션만 조회 (남의 세션이면 빈 값) */
    Optional<StudySession> findByIdAndUser_UserId(Long sessionId, Long userId);
}
