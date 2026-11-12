package com.onebyte.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** 메일 발송 등 @Async 사용. 이미 다른 곳에 @EnableAsync가 있으면 이 파일은 삭제 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
