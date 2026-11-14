package com.onebyte.backend.domain.studyrecord.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record StudyRecordPageResponse(
        List<Item> records,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public record Item(
            Long sessionId,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime startedAt,
            int recordedMinutes,
            /** 자료 없이 학습했거나 자료가 삭제된 경우 null */
            Long materialId,
            /** 학습 당시 자료 이름. 자료 없이 학습했으면 null */
            String materialName,
            ProblemStats problemStats,
            /** 해당 자료로 다시 학습할 수 있는지 (자료가 남아 있을 때만 true) */
            boolean reviewable
    ) {
    }

    /** 제출한 문제 기준 풀이 결과 */
    public record ProblemStats(int solvedCount, int correctCount, int wrongCount) {
    }
}
