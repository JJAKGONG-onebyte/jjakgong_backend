package com.onebyte.backend.domain.auth.event;

public record PasswordResetMailEvent(String email, String code) {
}
