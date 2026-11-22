package com.onebyte.backend.domain.schedule.repository;

import com.onebyte.backend.domain.schedule.entity.ScheduleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, Long> {

    /** 본인 일정만 조회 (남의 일정이면 빈 값) */
    Optional<ScheduleEvent> findByIdAndUser_Id(Long scheduleId, Long userId);

    /**
     * [startDate, endDate] 구간에 표시될 수 있는 본인 일정 후보.
     * - 일반 일정: 일정 날짜가 구간 안
     * - 반복 일정: 시작일이 구간 종료일 이전이거나, 날짜를 옮긴 회차가 구간 안에 있는 일정
     * 실제 발생일 계산과 반복 종료 조건은 ScheduleOccurrenceResolver에서 처리한다.
     */
    @Query("""
            select e from ScheduleEvent e
            where e.user.id = :userId
              and (
                    (e.scheduleDate >= :startDate and e.scheduleDate <= :endDate)
                 or (exists (select 1 from ScheduleRecurrenceRule r where r.scheduleId = e.id)
                     and (e.scheduleDate <= :endDate
                          or exists (select 1 from ScheduleOccurrence o
                                     where o.schedule = e
                                       and o.overrideDate >= :startDate
                                       and o.overrideDate <= :endDate)))
              )
            """)
    List<ScheduleEvent> findCandidatesInRange(@Param("userId") Long userId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
