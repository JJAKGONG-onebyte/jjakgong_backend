package com.onebyte.backend.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 반복 일정의 개별 회차 상태 (완료, 개별 수정, 개별 삭제) */
@Entity
@Table(name = "schedule_occurrence")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleOccurrence {

    @EmbeddedId
    private ScheduleOccurrenceId id;

    @MapsId("scheduleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ScheduleEvent schedule;

    @Column(nullable = false)
    private boolean isCompleted;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private boolean isDeleted;

    @Column(length = 150)
    private String overrideTitle;

    private LocalDate overrideDate;

    private LocalTime overrideTime;

    @Column(nullable = false)
    private boolean overrideAllDay;

    private LocalDate overrideDeadlineDate;

    @Column(nullable = false)
    private boolean clearDeadline;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SchedulePriority overridePriority;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ScheduleOccurrence of(ScheduleEvent schedule, LocalDate occurrenceDate) {
        ScheduleOccurrence occurrence = new ScheduleOccurrence();
        occurrence.id = new ScheduleOccurrenceId(schedule.getId(), occurrenceDate);
        occurrence.schedule = schedule;
        return occurrence;
    }
}
