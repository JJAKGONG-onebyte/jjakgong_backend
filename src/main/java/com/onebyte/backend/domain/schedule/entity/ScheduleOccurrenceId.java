package com.onebyte.backend.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/** schedule_occurrence 복합키 (schedule_id, occurrence_date) */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScheduleOccurrenceId implements Serializable {

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;
}
