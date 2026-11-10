package com.onebyte.backend.domain.schedule.entity;

import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScheduleEventType eventType;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    /** null이면 종일 일정 */
    private LocalTime scheduleTime;

    private LocalDate deadlineDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SchedulePriority priority;

    @Builder
    private ScheduleEvent(User user, String title, ScheduleEventType eventType, LocalDate scheduleDate,
                          LocalTime scheduleTime, LocalDate deadlineDate, SchedulePriority priority) {
        this.user = user;
        this.title = title;
        this.eventType = (eventType != null) ? eventType : ScheduleEventType.EVENT;
        this.scheduleDate = scheduleDate;
        this.scheduleTime = scheduleTime;
        this.deadlineDate = deadlineDate;
        this.priority = (priority != null) ? priority : SchedulePriority.MEDIUM;
    }
}
