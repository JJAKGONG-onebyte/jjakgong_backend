package com.onebyte.backend.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ===== 공통 =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // ===== 인증 =====
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해주세요."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않거나 만료되었습니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // ===== 캐릭터 =====
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 캐릭터입니다."),

    // ===== 학습 세션 =====
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "학습 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 학습 세션입니다."),
    ACTIVE_SESSION_EXISTS(HttpStatus.CONFLICT, "이미 진행 중인 학습 세션이 있습니다."),
    WEBCAM_DISABLED(HttpStatus.CONFLICT, "웹캠 사용이 꺼져 있어 집중 트래킹을 켤 수 없습니다."),
    NO_ACTIVE_SESSION(HttpStatus.NOT_FOUND, "진행 중인 학습 세션이 없습니다."),

    // ===== 학습 자료 =====
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 학습자료를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}