package com.onebyte.backend.domain.studysession.repository;

import com.onebyte.backend.domain.studysession.entity.StudyBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyBreakRepository extends JpaRepository<StudyBreak, Long> {

    List<StudyBreak> findAllBySession_Id(Long sessionId);
}
