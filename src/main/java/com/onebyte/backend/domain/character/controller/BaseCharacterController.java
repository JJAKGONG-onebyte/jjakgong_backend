package com.onebyte.backend.domain.character.controller;

import com.onebyte.backend.domain.character.dto.BaseCharacterListResponse;
import com.onebyte.backend.domain.character.service.BaseCharacterService;
import com.onebyte.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 가입 화면에서 쓰는 기본 캐릭터 목록 (로그인 전 호출이라 인증 불필요) */
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class BaseCharacterController {

    private final BaseCharacterService baseCharacterService;

    @GetMapping
    public ApiResponse<BaseCharacterListResponse> getBaseCharacters() {
        return ApiResponse.ok(baseCharacterService.getBaseCharacters());
    }
}
