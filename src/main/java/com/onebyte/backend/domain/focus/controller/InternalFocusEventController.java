package com.onebyte.backend.domain.focus.controller;

import com.onebyte.backend.domain.focus.dto.FocusEventRequest;
import com.onebyte.backend.domain.focus.dto.FocusEventResponse;
import com.onebyte.backend.domain.focus.service.FocusEventService;
import com.onebyte.backend.global.internal.InternalApiKeyInterceptor;
import com.onebyte.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "내부 API (AI 서버 전용)", description = "X-Internal-Api-Key 헤더로 인증. 브라우저에서 호출하지 않는다.")
@RestController
@RequestMapping("/internal/study-sessions")
@RequiredArgsConstructor
public class InternalFocusEventController {

    private final FocusEventService focusEventService;

    @Operation(summary = "집중 이벤트 기록", description = "AI 서버가 이벤트가 끝났을 때 한 번 호출한다.")
    @SecurityRequirements
    @Parameter(name = InternalApiKeyInterceptor.HEADER, in = ParameterIn.HEADER, required = true,
            description = "AI 서버 전용 서비스 키")
    @PostMapping("/{sessionId}/focus-events")
    public ResponseEntity<ApiResponse<FocusEventResponse>> record(
            @PathVariable Long sessionId,
            @RequestBody FocusEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(focusEventService.record(sessionId, request)));
    }
}
