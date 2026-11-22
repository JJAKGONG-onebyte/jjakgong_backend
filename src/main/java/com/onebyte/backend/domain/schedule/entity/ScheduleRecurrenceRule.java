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

    /** 반복 설정 변경 (endDate·count 둘 다 null이면 종료 조건 없는 반복) */
    public void change(RecurrenceFrequency frequency, LocalDate repeatEndDate, Integer repeatCount) {
        this.frequency = frequency;
        this.repeatEndDate = repeatEndDate;
        this.repeatCount = repeatCount;
    }

    /** 해당 날짜까지만 반복하도록 종료 (반복 시리즈 분할 시 기존 시리즈). 횟수 기준은 종료일 기준으로 바뀐다. */
    public void endAt(LocalDate repeatEndDate) {
        this.repeatEndDate = repeatEndDate;
        this.repeatCount = null;
    }
}
