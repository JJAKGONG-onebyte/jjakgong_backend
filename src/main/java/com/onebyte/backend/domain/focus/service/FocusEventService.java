package com.onebyte.backend.domain.focus.service;

import com.onebyte.backend.domain.focus.dto.FocusEventRequest;
import com.onebyte.backend.domain.focus.dto.FocusEventResponse;
import com.onebyte.backend.domain.focus.entity.FocusEvent;
import com.onebyte.backend.domain.focus.entity.FocusEventType;
import com.onebyte.backend.domain.focus.entity.FocusFeatureSample;
import com.onebyte.backend.domain.focus.repository.FocusEventRepository;
import com.onebyte.backend.domain.focus.repository.FocusFeatureSampleRepository;
import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.studysession.repository.StudySessionRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FocusEventService {

    /** AI 서버와 메인 서버의 시계 차이 허용 범위 */
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private final StudySessionRepository studySessionRepository;
    private final FocusEventRepository focusEventRepository;
    private final FocusFeatureSampleRepository focusFeatureSampleRepository;
    private final ObjectMapper objectMapper;

    /** 집중 이벤트 기록 (AI 서버 → 메인 서버, 이벤트가 끝났을 때 한 번) */
    @Transactional
    public FocusEventResponse record(Long sessionId, FocusEventRequest request) {
        FocusEventType eventType = parseEventType(request.eventType());
        validateRequired(request);
        boolean falsePositive = Boolean.TRUE.equals(request.falsePositive());
        if (falsePositive && request.featureSample() == null) {
            throw new CustomException(ErrorCode.MISSING_FEATURE_SAMPLE);
        }

        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isFocusTrackingEnabled()) {
            throw new CustomException(ErrorCode.FOCUS_TRACKING_DISABLED);
        }
        validateTime(session, request.startedAt(), request.endedAt());

        FocusEvent event = focusEventRepository.save(FocusEvent.builder()
                .session(session)
                .eventType(eventType)
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .durationSeconds((int) Duration.between(request.startedAt(), request.endedAt()).getSeconds())
                .falsePositive(falsePositive)
                .modelVersion(request.modelVersion())
                .confidence(request.confidence() == null ? null
                        : request.confidence().setScale(3, RoundingMode.HALF_UP))
                .build());

        if (request.featureSample() != null) {
            FocusEventRequest.FeatureSample sample = request.featureSample();
            focusFeatureSampleRepository.save(FocusFeatureSample.builder()
                    .event(event)
                    .windowSeconds(sample.windowSeconds())
                    .features(objectMapper.writeValueAsString(sample.features()))
                    .predictedLabel(sample.predictedLabel())
                    .correctedLabel(sample.correctedLabel())
                    .build());
        }
        return FocusEventResponse.from(event);
    }

    private FocusEventType parseEventType(String value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INVALID_EVENT_TYPE);
        }
        try {
            return FocusEventType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_EVENT_TYPE);
        }
    }

    private void validateRequired(FocusEventRequest request) {
        if (request.startedAt() == null || request.endedAt() == null) {
            throw new CustomException(ErrorCode.INVALID_EVENT_TIME);
        }
        if (request.modelVersion() == null || request.modelVersion().isBlank()
                || request.modelVersion().length() > 50) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "modelVersion은 1~50자로 필수입니다.");
        }
        BigDecimal confidence = request.confidence();
        if (confidence != null && (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "confidence는 0~1 사이여야 합니다.");
        }
        FocusEventRequest.FeatureSample sample = request.featureSample();
        if (sample != null && (sample.windowSeconds() == null || sample.windowSeconds() <= 0
                || sample.features() == null || sample.features().isEmpty()
                || sample.predictedLabel() == null || sample.predictedLabel().isBlank())) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "featureSample의 windowSeconds, features, predictedLabel은 필수입니다.");
        }
    }

    /**
     * 시작 ≤ 종료, 세션 시작 이후, 지금 이전.
     * 세션 종료 직후 도착한 이벤트도 종료 시각 이전에 끝났으면 저장한다.
     */
    private void validateTime(StudySession session, LocalDateTime startedAt, LocalDateTime endedAt) {
        LocalDateTime latest = session.isEnded() ? session.getEndedAt() : LocalDateTime.now();
        if (endedAt.isBefore(startedAt)
                || startedAt.isBefore(session.getStartedAt().minus(CLOCK_SKEW))
                || endedAt.isAfter(latest.plus(CLOCK_SKEW))) {
            throw new CustomException(ErrorCode.INVALID_EVENT_TIME);
        }
    }
}
