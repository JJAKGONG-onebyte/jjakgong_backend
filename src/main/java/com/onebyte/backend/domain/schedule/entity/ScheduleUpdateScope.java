package com.onebyte.backend.domain.schedule.entity;

/** 반복 일정 수정 범위 */
public enum ScheduleUpdateScope {
    /** 선택한 회차만 */
    THIS,
    /** 선택한 회차부터 이후 전체 */
    ALL_FUTURE
}
