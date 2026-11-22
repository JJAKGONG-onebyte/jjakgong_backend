package com.onebyte.backend.domain.schedule.repository;

import com.onebyte.backend.domain.schedule.entity.ScheduleRecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 반복 규칙 (PK = schedule_id) */
public interface ScheduleRecurrenceRuleRepository extends JpaRepository<ScheduleRecurrenceRule, Long> {

    /** 본인 일정의 반복 규칙 (반복 일정이 아니거나 남의 일정이면 빈 값) */
    @Query("select r from ScheduleRecurrenceRule r where r.scheduleId = :scheduleId and r.schedule.user.id = :userId")
    Optional<ScheduleRecurrenceRule> findByScheduleIdAndUserId(@Param("scheduleId") Long scheduleId,
                                                               @Param("userId") Long userId);

    /** 여러 일정의 반복 규칙 일괄 조회 (목록 조회 시 N+1 방지) */
    @Query("select r from ScheduleRecurrenceRule r where r.scheduleId in :scheduleIds and r.schedule.user.id = :userId")
    List<ScheduleRecurrenceRule> findAllByScheduleIdsAndUserId(@Param("scheduleIds") Collection<Long> scheduleIds,
                                                               @Param("userId") Long userId);
}
