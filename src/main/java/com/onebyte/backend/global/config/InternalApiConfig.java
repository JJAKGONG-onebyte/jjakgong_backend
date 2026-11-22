package com.onebyte.backend.global.config;

import com.onebyte.backend.global.internal.InternalApiKeyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** /internal/** 는 SecurityConfig에서 permitAll, 대신 여기서 서비스 키로 검증 */
@Configuration
@RequiredArgsConstructor
public class InternalApiConfig implements WebMvcConfigurer {

    private final InternalApiKeyInterceptor internalApiKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalApiKeyInterceptor).addPathPatterns("/internal/**");
    }
}
