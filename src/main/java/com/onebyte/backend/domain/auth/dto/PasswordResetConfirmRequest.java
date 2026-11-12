package com.onebyte.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "인증코드를 입력해주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자입니다.")
        String code,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "비밀번호는 8~64자이며 영문과 숫자를 포함해야 합니다.")
        String newPassword
) {
}
