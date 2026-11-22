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
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 2~20자여야 합니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 8~64자이며 영문과 숫자를 포함해야 합니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호가 기존 비밀번호와 같습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "기존 비밀번호가 일치하지 않습니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // ===== 캐릭터 =====
    INVALID_SLOT(HttpStatus.BAD_REQUEST, "지원하지 않는 슬롯이거나 슬롯에 맞지 않는 아이템입니다."),
    INVALID_COLOR(HttpStatus.BAD_REQUEST, "색상은 #RRGGBB 형식이어야 합니다."),
    ITEM_NOT_OWNED(HttpStatus.FORBIDDEN, "보유하지 않은 아이템입니다."),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 캐릭터입니다."),

    // ===== 일정 =====
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 일정을 찾을 수 없습니다."),
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "일정 제목은 필수 입력 항목입니다."),
    INVALID_RECURRENCE(HttpStatus.BAD_REQUEST, "반복 설정이 올바르지 않습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "조회 시작일은 종료일보다 늦을 수 없습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "날짜 형식은 YYYY-MM-DD이어야 합니다."),

    // ===== 학습 세션 =====
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "학습 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 학습 세션입니다."),
    ACTIVE_SESSION_EXISTS(HttpStatus.CONFLICT, "이미 진행 중인 학습 세션이 있습니다."),
    WEBCAM_DISABLED(HttpStatus.CONFLICT, "웹캠 사용이 꺼져 있어 집중 트래킹을 켤 수 없습니다."),
    NO_ACTIVE_SESSION(HttpStatus.NOT_FOUND, "진행 중인 학습 세션이 없습니다."),
    ALREADY_ON_BREAK(HttpStatus.CONFLICT, "이미 휴식 중입니다."),
    NOT_ON_BREAK(HttpStatus.CONFLICT, "진행 중인 휴식이 없습니다."),
    MISSING_ENABLED(HttpStatus.BAD_REQUEST, "enabled 값은 필수입니다."),

    // ===== 학습 자료 =====
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 학습자료를 찾을 수 없습니다."),
    MATERIAL_FILE_MISSING(HttpStatus.NOT_FOUND, "자료 파일을 찾을 수 없습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "비어 있는 파일은 업로드할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "PDF, TXT 파일만 업로드할 수 있습니다."),
    INVALID_FILE_CONTENT(HttpStatus.BAD_REQUEST, "파일 내용이 형식과 맞지 않습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 용량은 10MB 이하여야 합니다."),
    FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");

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