package com.onebyte.backend.domain.studyrecord.repository;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 학습 기록 전용 조회 레포지토리.
 * StudySessionRepository(학습화면 담당)와 파일이 겹치지 않도록 별도 인터페이스로 분리했다.
 */
public interface StudyRecordRepository extends Repository<StudySession, Long> {

    /** [from, to) 구간에 시작한, 종료된 본인 세션 (최신순) */
    @Query(value = """
            select new com.onebyte.backend.domain.studyrecord.repository.StudyRecordRow(
                s.id, s.startedAt, s.recordedMinutes, s.materialName, m.id)
            from StudySession s
            left join s.material m
            where s.user.id = :userId
              and s.endedAt is not null
              and s.recordedMinutes is not null
              and s.startedAt >= :from
              and s.startedAt < :to
            order by s.startedAt desc, s.id desc
            """,
            countQuery = """
            select count(s)
            from StudySession s
            where s.user.id = :userId
              and s.endedAt is not null
              and s.recordedMinutes is not null
              and s.startedAt >= :from
              and s.startedAt < :to
            """)
    Page<StudyRecordRow> findRecords(@Param("userId") Long userId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     Pageable pageable);

    /** 세션별 제출한 문제 수와 정답 수 (제출 기록이 없는 세션은 결과에 없음) */
    @Query("""
            select new com.onebyte.backend.domain.studyrecord.repository.SessionProblemCount(
                p.session.id, count(ps), sum(case when ps.correct = true then 1L else 0L end))
            from ProblemSubmission ps
            join ps.problem p
            where p.session.id in :sessionIds
            group by p.session.id
            """)
    List<SessionProblemCount> countProblemResults(@Param("sessionIds") Collection<Long> sessionIds);
}
