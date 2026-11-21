package com.onebyte.backend.domain.user.dto;

import jakarta.validation.constraints.NotNull;

/** 프로필 수정 (닉네임). 길이 검증은 서비스에서 함.*/
public record UpdateProfileRequest(
    @NotNull(message = "닉네임을 입력해주세요.")
    String nickname
){
}
