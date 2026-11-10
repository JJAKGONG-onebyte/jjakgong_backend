package com.onebyte.backend.domain.focus.entity;

import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 재학습용 특징값 샘플 (focus_event와 1:1, PK = event_id) */
@Entity
@Table(name = "focus_feature_sample")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusFeatureSample extends BaseEntity {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private FocusEvent event;

    @Column(nullable = false)
    private int windowSeconds;

    /** 구간 특징값 배열 (캘리브레이션 대비 변화량), JSON 문자열 */
    @Column(nullable = false, columnDefinition = "json")
    private String features;

    @Column(nullable = false, length = 20)
    private String predictedLabel;

    /** 사용자 교정 라벨. 버튼으로 답하지 않았으면 null */
    @Column(length = 20)
    private String correctedLabel;

    @Builder
    private FocusFeatureSample(FocusEvent event, int windowSeconds, String features,
                               String predictedLabel, String correctedLabel) {
        this.event = event;
        this.windowSeconds = windowSeconds;
        this.features = features;
        this.predictedLabel = predictedLabel;
        this.correctedLabel = correctedLabel;
    }
}
