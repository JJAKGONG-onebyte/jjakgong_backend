package com.onebyte.backend.domain.focus.entity;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "focus_event",
        indexes = @Index(name = "idx_focus_event_session", columnList = "session_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FocusEventType eventType;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private int durationSeconds;

    /** 사용자 오탐 신고 여부 */
    @Column(nullable = false)
    private boolean falsePositive;

    @Column(nullable = false, length = 50)
    private String modelVersion;

    /** 이벤트 구간 평균 예측 확률 (0~1) */
    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Builder
    private FocusEvent(StudySession session, FocusEventType eventType, LocalDateTime startedAt,
                       LocalDateTime endedAt, int durationSeconds, boolean falsePositive,
                       String modelVersion, BigDecimal confidence) {
        this.session = session;
        this.eventType = eventType;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.falsePositive = falsePositive;
        this.modelVersion = modelVersion;
        this.confidence = confidence;
    }
}
