package com.onebyte.backend.domain.character.controller;

import com.onebyte.backend.domain.character.dto.CharacterResponse;
import com.onebyte.backend.domain.character.dto.MyItemListResponse;
import com.onebyte.backend.domain.character.dto.SaveCharacterRequest;
import com.onebyte.backend.domain.character.service.CharacterService;
import com.onebyte.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserCharacterController {

    private final CharacterService characterService;

    @GetMapping("/character")
    public ApiResponse<CharacterResponse> getMyCharacter(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(characterService.getMyCharacter(userId));
    }

    @PutMapping("/character")
    public ApiResponse<Void> saveMyCharacter(@AuthenticationPrincipal Long userId,
                                             @Valid @RequestBody SaveCharacterRequest request) {
        characterService.saveMyCharacter(userId, request);
        return ApiResponse.ok();
    }

    @GetMapping("/items")
    public ApiResponse<MyItemListResponse> getMyItems(@AuthenticationPrincipal Long userId,
                                                      @RequestParam(required = false) String slot) {
        return ApiResponse.ok(characterService.getMyItems(userId, slot));
    }
}
