package com.onebyte.backend.domain.studysession.controller;

import com.onebyte.backend.domain.studysession.dto.ActiveSessionResponse;
import com.onebyte.backend.domain.studysession.dto.BreakEndResponse;
import com.onebyte.backend.domain.studysession.dto.BreakStartResponse;
import com.onebyte.backend.domain.studysession.dto.EndSessionResponse;
import com.onebyte.backend.domain.studysession.dto.HeartbeatResponse;
import com.onebyte.backend.domain.studysession.dto.SessionResponse;
import com.onebyte.backend.domain.studysession.dto.StartSessionRequest;
import com.onebyte.backend.domain.studysession.service.StudySessionService;
import com.onebyte.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학습 세션", description = "학습 시작·종료, 휴식, 진행 중 세션")
@RestController
@RequestMapping("/api/study-sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;

    @Operation(summary = "학습 시작", description = "학습 설정값을 스냅샷으로 저장하고 새 세션을 만든다. 요청 값은 모두 선택.")
    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> start(
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) StartSessionRequest request) {
        StartSessionRequest body = (request != null) ? request : new StartSessionRequest(null, null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(studySessionService.start(userId, body)));
    }

    @Operation(summary = "진행 중 세션 조회", description = "새로고침·재접속 시 경과 시간과 선택 자료를 복구한다. 10분 넘게 활동이 없던 세션은 자동 종료 후 404.")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ActiveSessionResponse>> getActive(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(studySessionService.getActive(userId)));
    }

    @Operation(summary = "세션 활동 유지 (heartbeat)", description = "학습 화면이 1분마다 호출해 마지막 활동 시각을 갱신한다.")
    @PostMapping("/{sessionId}/heartbeat")
    public ResponseEntity<ApiResponse<HeartbeatResponse>> heartbeat(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(studySessionService.heartbeat(sessionId, userId)));
    }

    @Operation(summary = "휴식 시작", description = "진행 중 세션에서 휴식을 시작한다. 휴식 중에도 heartbeat는 계속 보내야 한다.")
    @PostMapping("/{sessionId}/breaks")
    public ResponseEntity<ApiResponse<BreakStartResponse>> startBreak(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(studySessionService.startBreak(sessionId, userId)));
    }

    @Operation(summary = "휴식 종료", description = "진행 중인 휴식을 끝내고 누적 휴식 시간을 반환한다.")
    @PostMapping("/{sessionId}/breaks/end")
    public ResponseEntity<ApiResponse<BreakEndResponse>> endBreak(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(studySessionService.endBreak(sessionId, userId)));
    }

    @Operation(summary = "학습 종료", description = "학습 시간(휴식 제외)을 기록하고 문제 풀이·집중 이벤트 통계를 반환한다.")
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<EndSessionResponse>> end(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(studySessionService.end(sessionId, userId)));
    }
}
