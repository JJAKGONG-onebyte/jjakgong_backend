package com.onebyte.backend.domain.studysession.repository;

import com.onebyte.backend.domain.studysession.entity.StudyBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyBreakRepository extends JpaRepository<StudyBreak, Long> {

    List<StudyBreak> findAllBySession_Id(Long sessionId);

    /** 세션의 진행 중인 휴식 */
    Optional<StudyBreak> findFirstBySession_IdAndEndedAtIsNull(Long sessionId);
}
