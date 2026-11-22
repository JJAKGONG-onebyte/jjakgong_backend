package com.onebyte.backend.domain.dashboard.repository;

import com.onebyte.backend.domain.material.entity.StudyMaterial;
import com.onebyte.backend.domain.studysession.entity.StudySession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 전용 조회 레포지토리.
 * 학습 세션·학습 자료 담당자의 Repository와 파일이 겹치지 않도록 별도 인터페이스로 분리했다. (StudyStatsRepository와 같은 방식)
 */
public interface DashboardQueryRepository extends Repository<StudySession, Long> {

    /** [from, to) 구간에 시작한, 종료된 세션의 학습 시간(분) 합계. 진행 중 세션은 제외 */
    @Query("""
            select coalesce(sum(s.recordedMinutes), 0)
            from StudySession s
            where s.user.id = :userId
              and s.endedAt is not null
              and s.recordedMinutes is not null
              and s.startedAt >= :from
              and s.startedAt < :to
            """)
    Long sumFinishedMinutes(@Param("userId") Long userId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    /** 종료된 본인 세션 (최근 종료순) */
    @Query("""
            select new com.onebyte.backend.domain.dashboard.repository.RecentStudyRecordRow(
                s.id, s.materialName, s.recordedMinutes, s.endedAt)
            from StudySession s
            where s.user.id = :userId
              and s.endedAt is not null
              and s.recordedMinutes is not null
            order by s.endedAt desc, s.id desc
            """)
    List<RecentStudyRecordRow> findRecentFinished(@Param("userId") Long userId, Pageable pageable);

    /** 본인 학습 자료 (최근 업로드순) */
    @Query("""
            select m from StudyMaterial m
            where m.user.id = :userId
            order by m.uploadedAt desc, m.id desc
            """)
    List<StudyMaterial> findRecentMaterials(@Param("userId") Long userId, Pageable pageable);

    /** 학습할 자료로 선택된 자료 ID (사용자당 최대 1개) */
    @Query("select m.id from StudyMaterial m where m.user.id = :userId and m.selected = true order by m.id")
    List<Long> findSelectedMaterialIds(@Param("userId") Long userId);
}
