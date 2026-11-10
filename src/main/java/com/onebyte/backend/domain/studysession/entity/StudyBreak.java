package com.onebyte.backend.domain.studysession.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_break",
        indexes = @Index(name = "idx_break_session_ended", columnList = "session_id, ended_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyBreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "break_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    /** null이면 휴식 중 */
    private LocalDateTime endedAt;

    public static StudyBreak start(StudySession session) {
        StudyBreak studyBreak = new StudyBreak();
        studyBreak.session = session;
        studyBreak.startedAt = LocalDateTime.now();
        return studyBreak;
    }
}
