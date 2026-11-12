package com.onebyte.backend.domain.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 재설정 코드가 DB에 커밋된 뒤, 별도 스레드에서 메일을 보낸다.
 * (메일 발송 시간 때문에 가입된 이메일 / 미가입 이메일의 응답 시간이 달라지는 것을 막기 위함)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailListener {

    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${app.mail.from:}")
    private String from;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PasswordResetMailEvent event) {
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            // SMTP 설정 없이 로컬에서 테스트하기 위한 로그 (local 프로필에서만)
            log.info("[LOCAL] 비밀번호 재설정 코드 email={}, code={}", event.email(), event.code());
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (!from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(event.email());
            message.setSubject("[짝공] 비밀번호 재설정 인증코드");
            message.setText("인증코드: " + event.code() + "\n10분 안에 입력해주세요.\n본인이 요청하지 않았다면 이 메일을 무시해주세요.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패", e);
        }
    }
}
