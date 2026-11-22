package com.onebyte.backend.domain.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.onebyte.backend.domain.character.dto.CharacterResponse;
import com.onebyte.backend.domain.dashboard.repository.RecentStudyRecordRow;
import com.onebyte.backend.domain.material.entity.StudyMaterial;
import com.onebyte.backend.domain.schedule.entity.SchedulePriority;
import com.onebyte.backend.domain.schedule.service.ResolvedOccurrence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** 홈/대시보드 통합 조회 응답 */
public record DashboardResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        UserInfo user,
        StudyTime studyTime,
        Streak streak,
        CharacterInfo character,
        List<TodaySchedule> todaySchedules,
        List<LearningMaterial> learningMaterials,
        List<RecentStudyRecord> recentStudyRecords,
        Long defaultLearningMaterialId
) {

    public record UserInfo(String displayName) {
    }

    /** 초 단위 (학습 세션은 분 단위로 기록되므로 60의 배수) */
    public record StudyTime(long todaySeconds, long goalSeconds, int achievementRate) {
    }

    public record Streak(int days) {
    }

    /** 캐릭터 조회 API(CharacterService)와 같은 레벨 계산 결과 */
    public record CharacterInfo(
            Long characterId,
            int level,
            int minutesToNextLevel,
            int levelProgressPercent,
            String appearanceUrl
    ) {
        public static CharacterInfo from(CharacterResponse c) {
            return new CharacterInfo(c.characterId(), c.level(), c.minutesToNextLevel(),
                    c.levelProgressPercent(), c.characterImageUrl());
        }
    }

    public record TodaySchedule(
            Long scheduleId,
            String title,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime time,
            SchedulePriority priority,
            boolean completed,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            LocalDate occurrenceDate
    ) {
        public static TodaySchedule from(ResolvedOccurrence o) {
            return new TodaySchedule(o.scheduleId(), o.title(), o.time(), o.priority(), o.completed(),
                    o.occurrenceDate());
        }
    }

    public record LearningMaterial(
            Long materialId,
            String fileName,
            String fileType,
            long fileSizeBytes,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime uploadedAt
    ) {
        public static LearningMaterial from(StudyMaterial m) {
            return new LearningMaterial(m.getId(), m.getName(), m.getFileType(), m.getFileSize(), m.getUploadedAt());
        }
    }

    /** title은 세션에 저장된 자료 이름 (자료 없이 학습했으면 null) */
    public record RecentStudyRecord(
            Long sessionId,
            String title,
            long durationSeconds,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime endedAt
    ) {
        public static RecentStudyRecord from(RecentStudyRecordRow row) {
            return new RecentStudyRecord(row.sessionId(), row.materialName(),
                    row.recordedMinutes() * 60L, row.endedAt());
        }
    }
}
