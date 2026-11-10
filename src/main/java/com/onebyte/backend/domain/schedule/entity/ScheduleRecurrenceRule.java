package com.onebyte.backend.domain.schedule.entity;

import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 반복 규칙 (schedule_event와 1:1, PK = schedule_id) */
@Entity
@Table(name = "schedule_recurrence_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleRecurrenceRule extends BaseEntity {

    @Id
    @Column(name = "schedule_id")
    private Long scheduleId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ScheduleEvent schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurrenceFrequency frequency;

    @Column(nullable = false)
    private int repeatInterval;

    private LocalDate repeatEndDate;

    private Integer repeatCount;

    @Builder
    private ScheduleRecurrenceRule(ScheduleEvent schedule, RecurrenceFrequency frequency, Integer repeatInterval,
                                   LocalDate repeatEndDate, Integer repeatCount) {
        this.schedule = schedule;
        this.frequency = frequency;
        this.repeatInterval = (repeatInterval != null) ? repeatInterval : 1;
        this.repeatEndDate = repeatEndDate;
        this.repeatCount = repeatCount;
    }
}
