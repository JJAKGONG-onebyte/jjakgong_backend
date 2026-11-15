package com.onebyte.backend.domain.material.service;

import org.springframework.http.MediaType;

import java.io.InputStream;

/** 파일 조회 API가 내려줄 파일 정보. stream은 받는 쪽에서 닫는다. */
public record MaterialFile(String fileName, MediaType mediaType, long size, InputStream stream) {
}
