package com.onebyte.backend.domain.studysession.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_break",
        indexes = @Index(name = "idx_break_session_ended", columnList = "session_id, ended_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyBreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "break_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    /** null이면 휴식 중 */
    private LocalDateTime endedAt;

    public static StudyBreak start(StudySession session) {
        StudyBreak studyBreak = new StudyBreak();
        studyBreak.session = session;
        studyBreak.startedAt = LocalDateTime.now();
        return studyBreak;
    }

    public boolean isOngoing() {
        return endedAt == null;
    }

    public void end(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    /** until 시각까지의 휴식 시간(초). 진행 중인 휴식은 until까지로 계산 */
    public long durationSecondsUntil(LocalDateTime until) {
        LocalDateTime end = (endedAt != null && endedAt.isBefore(until)) ? endedAt : until;
        return Math.max(0, java.time.Duration.between(startedAt, end).getSeconds());
    }
}
