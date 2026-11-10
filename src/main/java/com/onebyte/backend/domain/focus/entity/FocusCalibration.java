package com.onebyte.backend.domain.focus.entity;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 캘리브레이션 기준값 (study_session과 1:1, PK = session_id) */
@Entity
@Table(name = "focus_calibration")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusCalibration {

    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private StudySession session;

    /** 화면 볼 때 평균 특징값, JSON 문자열 */
    @Column(nullable = false, columnDefinition = "json")
    private String screenView;

    /** 책·노트 볼 때 평균 특징값, JSON 문자열 (없으면 null) */
    @Column(columnDefinition = "json")
    private String materialView;

    @Column(nullable = false)
    private int sampleCount;

    @Column(nullable = false)
    private LocalDateTime calibratedAt;

    @Builder
    private FocusCalibration(StudySession session, String screenView, String materialView, int sampleCount) {
        this.session = session;
        this.screenView = screenView;
        this.materialView = materialView;
        this.sampleCount = sampleCount;
        this.calibratedAt = LocalDateTime.now();
    }
}
