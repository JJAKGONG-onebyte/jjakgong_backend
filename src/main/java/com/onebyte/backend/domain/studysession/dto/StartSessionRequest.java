package com.onebyte.backend.domain.studysession.dto;

import com.onebyte.backend.domain.studysetting.entity.ScreenMode;

/**
 * 학습 시작 요청. 모든 값은 선택이며, 없으면 학습 설정값을 사용한다.
 *
 * @param materialId           이번 세션에 사용할 학습자료 ID (없으면 자료 없이 학습)
 * @param pomodoroEnabled      이번 세션 뽀모도로 사용 여부 (기본 true)
 * @param focusTrackingEnabled 이번 세션 집중 트래킹 사용 여부 (기본: 설정의 트래킹 && 웹캠)
 * @param screenMode           학습 화면 모드 (기본: 설정의 마지막 모드)
 */
public record StartSessionRequest(
        Long materialId,
        Boolean pomodoroEnabled,
        Boolean focusTrackingEnabled,
        ScreenMode screenMode
) {
}
