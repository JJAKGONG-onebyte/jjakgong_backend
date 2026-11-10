package com.onebyte.backend.domain.problem.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 답안 제출 (문제당 1번) */
@Entity
@Table(name = "problem_submission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false, unique = true)
    private Problem problem;

    @Column(nullable = false, length = 200)
    private String submittedAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Builder
    private ProblemSubmission(Problem problem, String submittedAnswer, boolean correct) {
        this.problem = problem;
        this.submittedAnswer = submittedAnswer;
        this.correct = correct;
        this.submittedAt = LocalDateTime.now();
    }
}
