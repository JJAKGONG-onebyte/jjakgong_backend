package com.onebyte.backend.domain.studysession.entity;

import com.onebyte.backend.domain.material.entity.StudyMaterial;
import com.onebyte.backend.domain.studysetting.entity.FocusSensitivity;
import com.onebyte.backend.domain.studysetting.entity.ScreenMode;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_session",
        indexes = {
                @Index(name = "idx_session_user_ended", columnList = "user_id, ended_at"),
                @Index(name = "idx_session_ended_activity", columnList = "ended_at, last_activity_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudySession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 이번 세션의 학습자료 (한 개). 자료 없이 학습하거나 자료가 삭제되면 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private StudyMaterial material;

    /** 자료 이름 스냅샷 (자료 삭제 후에도 기록 표시용) */
    @Column(length = 255)
    private String materialName;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    /** null이면 진행 중 */
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SessionEndType endType;

    /** heartbeat 등으로 갱신. 비정상 종료 시 이 시각까지를 학습 시간으로 기록 */
    @Column(nullable = false)
    private LocalDateTime lastActivityAt;

    /** 실제로 기록된 학습 시간(분), 휴식 제외. 종료 시 계산 */
    private Integer recordedMinutes;

    // ===== 세션 시작 시점 설정 스냅샷 =====

    /** 세션당 학습 시간 설정값(분) */
    @Column(nullable = false)
    private int studyMinutes;

    @Column(nullable = false)
    private int breakMinutes;

    @Column(nullable = false)
    private boolean pomodoroEnabled;

    /** 현재 집중 트래킹 사용 여부 (학습 중 변경 반영) */
    @Column(nullable = false)
    private boolean focusTrackingEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FocusSensitivity focusSensitivity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScreenMode screenMode;

    @Builder
    private StudySession(User user, StudyMaterial material, int studyMinutes, int breakMinutes,
                         boolean pomodoroEnabled, boolean focusTrackingEnabled,
                         FocusSensitivity focusSensitivity, ScreenMode screenMode) {
        LocalDateTime now = LocalDateTime.now();
        this.user = user;
        this.material = material;
        this.materialName = (material != null) ? material.getName() : null;
        this.startedAt = now;
        this.lastActivityAt = now;
        this.studyMinutes = studyMinutes;
        this.breakMinutes = breakMinutes;
        this.pomodoroEnabled = pomodoroEnabled;
        this.focusTrackingEnabled = focusTrackingEnabled;
        this.focusSensitivity = (focusSensitivity != null) ? focusSensitivity : FocusSensitivity.NORMAL;
        this.screenMode = (screenMode != null) ? screenMode : ScreenMode.VIDEO_CALL;
    }

    /** 종료된 세션인지 */
    public boolean isEnded() {
        return endedAt != null;
    }

    /** 마지막 활동 시각 갱신 (heartbeat, 질문, 문제 생성 등 세션 관련 요청마다) */
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}
