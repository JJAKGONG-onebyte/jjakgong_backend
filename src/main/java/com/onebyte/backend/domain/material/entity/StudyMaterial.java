package com.onebyte.backend.domain.material.entity;

import com.onebyte.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_material")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 표시 이름 (사용자가 변경 가능) */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String originFileName;

    /** PDF / TXT */
    @Column(nullable = false, length = 10)
    private String fileType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 500)
    private String filePath;

    /** 학습 시작 모달에서 미리 선택해 둘 기본 자료 (사용자당 최대 1개) */
    @Column(nullable = false)
    private boolean selected;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Builder
    private StudyMaterial(User user, String name, String originFileName, String fileType,
                          long fileSize, String filePath) {
        this.user = user;
        this.name = name;
        this.originFileName = originFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.selected = false;
        this.uploadedAt = LocalDateTime.now();
    }
}
