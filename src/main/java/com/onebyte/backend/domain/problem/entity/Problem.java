package com.onebyte.backend.domain.problem.entity;

import com.onebyte.backend.domain.material.entity.StudyMaterial;
import com.onebyte.backend.domain.studysession.entity.StudySession;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "problem",
        indexes = {
                @Index(name = "idx_problem_session", columnList = "session_id"),
                @Index(name = "idx_problem_user", columnList = "user_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "problem_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    /** 복습 조회용 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 근거 자료. 자료가 삭제되면 null (풀이 기록은 유지) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private StudyMaterial material;

    @Column(nullable = false)
    private String materialName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemType problemType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    /** 객관식 보기 [{optionId, text}] JSON 문자열, 단답형은 null */
    @Column(columnDefinition = "json")
    private String options;

    /** 정답 (객관식은 optionId) */
    @Column(nullable = false, length = 200)
    private String correctAnswer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Builder
    private Problem(StudySession session, User user, StudyMaterial material, ProblemType problemType,
                    String question, String options, String correctAnswer, String explanation) {
        this.session = session;
        this.user = user;
        this.material = material;
        this.materialName = (material != null) ? material.getName() : null;
        this.problemType = problemType;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }
}
