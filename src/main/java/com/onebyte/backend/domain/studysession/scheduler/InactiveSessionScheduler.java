package com.onebyte.backend.domain.studysession.scheduler;

import com.onebyte.backend.domain.studysession.repository.StudySessionRepository;
import com.onebyte.backend.domain.studysession.service.StudySessionCloser;
import com.onebyte.backend.domain.studysession.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 브라우저를 닫는 등 heartbeat가 끊긴 세션을 1분마다 찾아 자동 종료(AUTO)한다.
 * 세션마다 별도 트랜잭션으로 처리해서 한 세션이 실패해도 나머지는 계속 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveSessionScheduler {

    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void closeInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minus(StudySessionCloser.INACTIVE_LIMIT);
        List<Long> sessionIds = studySessionRepository.findInactiveSessionIds(threshold);
        if (sessionIds.isEmpty()) {
            return;
        }

        int closed = 0;
        for (Long sessionId : sessionIds) {
            try {
                if (studySessionService.closeIfInactive(sessionId)) {
                    closed++;
                }
            } catch (Exception e) {
                log.error("비활성 세션 자동 종료 실패 sessionId={}", sessionId, e);
            }
        }
        log.info("비활성 세션 자동 종료 {}건 / 대상 {}건", closed, sessionIds.size());
    }
}
