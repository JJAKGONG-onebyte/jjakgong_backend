
package com.onebyte.backend.domain.studysession.service;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.studysession.repository.StudySessionRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인(문제, AI 질문, 집중 이벤트 등)에서 sessionId로 세션을 확인할 때 쓰는 공통 조회기.
 *
 * <pre>
 * StudySession session = studySessionFinder.getActiveSession(sessionId, userId);
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudySessionFinder {

    private final StudySessionRepository studySessionRepository;

    /**
     * 본인 세션 조회 (종료 여부와 무관). 조회 API, 기록 조회 등에 사용.
     *
     * @throws CustomException 404 SESSION_NOT_FOUND: 없거나 다른 사용자의 세션
     */
    public StudySession getOwnedSession(Long sessionId, Long userId) {
        return studySessionRepository.findByIdAndUser_UserId(sessionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
    }

    /**
     * 진행 중인 본인 세션 조회 + 마지막 활동 시각 갱신.
     * 질문, 문제 생성처럼 학습 중에만 가능한 요청에 사용.
     *
     * @throws CustomException 404 SESSION_NOT_FOUND, 409 SESSION_ALREADY_ENDED
     */
    @Transactional
    public StudySession getActiveSession(Long sessionId, Long userId) {
        StudySession session = getOwnedSession(sessionId, userId);
        return touchIfActive(session);
    }

    /**
     * AI 서버가 호출하는 내부 API(/internal/**)용. 사용자 확인 없이 세션 ID로만 조회.
     *
     * @throws CustomException 404 SESSION_NOT_FOUND, 409 SESSION_ALREADY_ENDED
     */
    @Transactional
    public StudySession getActiveSessionForInternal(Long sessionId) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
        return touchIfActive(session);
    }

    private StudySession touchIfActive(StudySession session) {
        if (session.isEnded()) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        session.updateLastActivity();
        return session;
    }
}
