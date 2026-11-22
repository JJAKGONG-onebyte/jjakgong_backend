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

    // ===== 개별 수정 (선택한 회차만 수정) =====

    public void overrideTitle(String title) {
        this.overrideTitle = title;
    }

    /** 표시 날짜 변경. null이면 원래 발생일로 되돌린다 */
    public void overrideDate(LocalDate date) {
        this.overrideDate = date;
    }

    public void overrideTime(LocalTime time) {
        this.overrideTime = time;
        this.overrideAllDay = false;
    }

    /** 이 회차만 종일 일정으로 변경 */
    public void overrideAllDay() {
        this.overrideTime = null;
        this.overrideAllDay = true;
    }

    public void overrideDeadline(LocalDate deadlineDate) {
        this.overrideDeadlineDate = deadlineDate;
        this.clearDeadline = false;
    }

    /** 이 회차만 마감일 삭제 */
    public void clearDeadline() {
        this.overrideDeadlineDate = null;
        this.clearDeadline = true;
    }

    public void overridePriority(SchedulePriority priority) {
        this.overridePriority = priority;
    }

    /** 개별 수정 값을 모두 지우고 반복 일정 값을 따르게 한다 (완료·삭제 상태는 유지) */
    public void resetOverrides() {
        this.overrideTitle = null;
        this.overrideDate = null;
        this.overrideTime = null;
        this.overrideAllDay = false;
        this.overrideDeadlineDate = null;
        this.clearDeadline = false;
        this.overridePriority = null;
    }

    // ===== 완료 / 개별 삭제 =====

    /** 완료 처리 (이미 완료된 회차면 완료 시각을 유지) */
    public void complete(LocalDateTime completedAt) {
        if (!this.isCompleted) {
            this.completedAt = completedAt;
        }
        this.isCompleted = true;
    }

    /** 완료 취소 */
    public void cancelCompletion() {
        this.isCompleted = false;
        this.completedAt = null;
    }

    /** 이 회차만 삭제 (반복 규칙은 유지) */
    public void markDeleted() {
        this.isDeleted = true;
    }

    /** 완료·삭제 상태 복사 (반복 시리즈를 나누거나 날짜를 옮길 때 회차 상태 이전) */
    public void copyStatusFrom(ScheduleOccurrence other) {
        this.isCompleted = other.isCompleted;
        this.completedAt = other.completedAt;
        this.isDeleted = other.isDeleted;
    }

    /** 완료·삭제 상태가 있는 회차인지 (개별 수정만 있는 회차는 false) */
    public boolean hasStatus() {
        return isCompleted || isDeleted;
    }
}
