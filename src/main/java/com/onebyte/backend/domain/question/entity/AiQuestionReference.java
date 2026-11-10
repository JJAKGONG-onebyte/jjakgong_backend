package com.onebyte.backend.domain.question.entity;

import com.onebyte.backend.domain.material.entity.StudyMaterial;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** AI 답변 근거 자료 */
@Entity
@Table(name = "ai_question_reference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiQuestionReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reference_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AiQuestion question;

    /** 자료가 삭제되면 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private StudyMaterial material;

    @Column(nullable = false)
    private String materialName;

    /** PDF 페이지 번호 (TXT이거나 알 수 없으면 null) */
    private Integer page;

    @Builder
    private AiQuestionReference(AiQuestion question, StudyMaterial material, Integer page) {
        this.question = question;
        this.material = material;
        this.materialName = (material != null) ? material.getName() : null;
        this.page = page;
    }
}
