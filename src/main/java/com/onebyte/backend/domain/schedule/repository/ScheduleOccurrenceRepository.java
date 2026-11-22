package com.onebyte.backend.domain.schedule.repository;

import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrence;
import com.onebyte.backend.domain.schedule.entity.ScheduleOccurrenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 일정 회차별 상태 (완료, 개별 수정, 개별 삭제). 식별 기준은 scheduleId + occurrenceDate */
public interface ScheduleOccurrenceRepository extends JpaRepository<ScheduleOccurrence, ScheduleOccurrenceId> {

    /** 본인 일정의 특정 회차 상태 (기록이 없거나 남의 일정이면 빈 값) */
    @Query("""
            select o from ScheduleOccurrence o
            where o.id.scheduleId = :scheduleId
              and o.id.occurrenceDate = :occurrenceDate
              and o.schedule.user.id = :userId
            """)
    Optional<ScheduleOccurrence> findOne(@Param("scheduleId") Long scheduleId,
                                         @Param("occurrenceDate") LocalDate occurrenceDate,
                                         @Param("userId") Long userId);

    /**
     * 여러 일정의 회차 상태 중 [startDate, endDate] 구간과 관련된 것.
     * 발생일이 구간 안이거나, 날짜를 옮긴 결과(overrideDate)가 구간 안인 회차를 모두 가져온다.
     */
    @Query("""
            select o from ScheduleOccurrence o
            where o.id.scheduleId in :scheduleIds
              and o.schedule.user.id = :userId
              and (
                    (o.id.occurrenceDate >= :startDate and o.id.occurrenceDate <= :endDate)
                 or (o.overrideDate >= :startDate and o.overrideDate <= :endDate)
              )
            """)
    List<ScheduleOccurrence> findAllInRange(@Param("scheduleIds") Collection<Long> scheduleIds,
                                            @Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
