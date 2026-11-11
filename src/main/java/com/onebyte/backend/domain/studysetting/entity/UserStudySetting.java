package com.onebyte.backend.domain.studysetting.entity;

import com.onebyte.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** 사용자별 학습 설정 (users와 1:1, PK = user_id) */
@Entity
@Table(name = "user_study_setting")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStudySetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** 세션당 학습 시간(분) */
    @Column(nullable = false)
    private int studyMinutes;

    /** 휴식 시간(분) */
    @Column(nullable = false)
    private int breakMinutes;

    @Column(nullable = false)
    private boolean focusTrackingEnabled;

    @Column(nullable = false)
    private boolean webcamEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FocusSensitivity focusSensitivity;

    /** 마지막으로 사용한 학습 화면 모드 (다음 학습 때 기본값) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScreenMode screenMode;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 기본값으로 생성: 50분 / 10분 / 트래킹 OFF / 웹캠 ON / NORMAL / 영상 통화형 */
    public static UserStudySetting createDefault(User user) {
        UserStudySetting setting = new UserStudySetting();
        setting.user = user;
        setting.studyMinutes = 50;
        setting.breakMinutes = 10;
        setting.focusTrackingEnabled = false;
        setting.webcamEnabled = true;
        setting.focusSensitivity = FocusSensitivity.NORMAL;
        setting.screenMode = ScreenMode.VIDEO_CALL;
        return setting;
    }

    public void changeScreenMode(ScreenMode screenMode) {
        this.screenMode = screenMode;
    }
}
