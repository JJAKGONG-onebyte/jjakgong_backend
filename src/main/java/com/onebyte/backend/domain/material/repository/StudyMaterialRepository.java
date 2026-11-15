package com.onebyte.backend.domain.material.repository;

import com.onebyte.backend.domain.material.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    /** 본인 자료 목록 (최신 업로드순) */
    List<StudyMaterial> findByUser_IdOrderByUploadedAtDescIdDesc(Long userId);

    /** 본인 자료만 조회 (남의 자료면 빈 값) */
    Optional<StudyMaterial> findByIdAndUser_Id(Long materialId, Long userId);

    boolean existsByIdAndUser_Id(Long materialId, Long userId);

    /** 해당 자료만 선택 상태로 만들고 나머지는 모두 해제 (사용자당 선택은 최대 1개) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update StudyMaterial m
            set m.selected = case when m.id = :materialId then true else false end
            where m.user.id = :userId
            """)
    int selectOnly(@Param("userId") Long userId, @Param("materialId") Long materialId);

    /** 선택 해제 (자료 없이 학습) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StudyMaterial m set m.selected = false where m.user.id = :userId and m.selected = true")
    int clearSelected(@Param("userId") Long userId);
}
