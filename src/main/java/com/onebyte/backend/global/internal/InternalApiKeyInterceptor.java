package com.onebyte.backend.global.internal;

import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /internal/** 요청의 서비스 키 검증 (AI 서버 → 메인 서버).
 * 키가 설정되지 않았으면 모든 요청을 막는다.
 */
@Component
public class InternalApiKeyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Internal-Api-Key";

    private final byte[] apiKey;

    public InternalApiKeyInterceptor(@Value("${internal.api-key:}") String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String given = request.getHeader(HEADER);
        if (apiKey.length == 0 || given == null
                || !MessageDigest.isEqual(apiKey, given.getBytes(StandardCharsets.UTF_8))) {
            throw new CustomException(ErrorCode.INVALID_INTERNAL_KEY);
        }
        return true;
    }
}
