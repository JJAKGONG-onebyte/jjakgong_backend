package com.onebyte.backend.domain.focus.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 서버가 보내는 집중 이벤트.
 * eventType은 정의되지 않은 값일 때 INVALID_EVENT_TYPE으로 응답하기 위해 String으로 받는다.
 */
public record FocusEventRequest(
        String eventType,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Boolean falsePositive,
        String modelVersion,
        BigDecimal confidence,
        FeatureSample featureSample
) {
    public record FeatureSample(
            Integer windowSeconds,
            List<Map<String, Object>> features,
            String predictedLabel,
            String correctedLabel
    ) {
    }
}
