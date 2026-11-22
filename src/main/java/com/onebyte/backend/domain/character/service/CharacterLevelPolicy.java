package com.onebyte.backend.domain.character.service;

import org.springframework.stereotype.Component;

/**
 * 캐릭터 레벨 계산.
 *
 *  기획에서 레벨별 필요 학습 시간과 최고 레벨이 확정되면 아래 상수/식만 바꾸면 됨.
 *  (지금은 임시 기준: 레벨 L에 도달하려면 누적 30 * L * (L - 1)분, 최고 레벨 10)
 */
@Component
public class CharacterLevelPolicy {

    static final int MAX_LEVEL = 10;
    private static final int BASE_MINUTES = 30;

    public LevelInfo calculate(long totalMinutes) {
        long total = Math.max(0, totalMinutes);

        int level = 1;
        while (level < MAX_LEVEL && total >= requiredTotal(level + 1)) {
            level++;
        }

        if (level >= MAX_LEVEL) {
            // 최고 레벨: 남은 시간 0, 게이지 가득
            return new LevelInfo(level, 0, 100);
        }

        long start = requiredTotal(level);
        long next = requiredTotal(level + 1);
        long section = next - start;
        long done = total - start;

        int minutesToNext = (int) (next - total);                 // 항상 1분 이상이라 올림 이슈 없음
        int progressPercent = (int) (done * 100 / section);        // 0~99 (버림)
        return new LevelInfo(level, minutesToNext, progressPercent);
    }

    /** 해당 레벨에 도달하기 위한 누적 학습 시간(분). 레벨 1은 0분 */
    private long requiredTotal(int level) {
        return (long) BASE_MINUTES * level * (level - 1);
    }

    public record LevelInfo(int level, int minutesToNextLevel, int levelProgressPercent) {
    }
}