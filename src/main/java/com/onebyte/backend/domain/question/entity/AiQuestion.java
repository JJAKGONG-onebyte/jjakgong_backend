package com.onebyte.backend.domain.question.entity;

import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_question",
        indexes = @Index(name = "idx_ai_question_session", columnList = "session_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType questionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 설명 요청 구절 (EXPLAIN일 때) */
    @Column(columnDefinition = "TEXT")
    private String targetText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private boolean basedOnMaterials;

    @Builder
    private AiQuestion(StudySession session, QuestionType questionType, String content,
                       String targetText, String answer, boolean basedOnMaterials) {
        this.session = session;
        this.questionType = questionType;
        this.content = content;
        this.targetText = targetText;
        this.answer = answer;
        this.basedOnMaterials = basedOnMaterials;
    }
}
