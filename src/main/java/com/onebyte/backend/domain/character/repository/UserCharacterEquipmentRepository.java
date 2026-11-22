package com.onebyte.backend.domain.character.repository;

import com.onebyte.backend.domain.character.entity.UserCharacterEquipment;
import com.onebyte.backend.domain.character.entity.UserCharacterEquipmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserCharacterEquipmentRepository
        extends JpaRepository<UserCharacterEquipment, UserCharacterEquipmentId> {

    @Query("""
            select e from UserCharacterEquipment e join fetch e.item
            where e.id.userId = :userId
            """)
    List<UserCharacterEquipment> findAllWithItem(@Param("userId") Long userId);

    /** 저장하기(전체 교체) 전에 기존 장착 현황을 한 번에 비운다. 영속성 컨텍스트도 함께 비움 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserCharacterEquipment e where e.id.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}