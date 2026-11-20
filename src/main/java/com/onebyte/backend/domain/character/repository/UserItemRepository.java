package com.onebyte.backend.domain.character.repository;

import com.onebyte.backend.domain.character.entity.UserItem;
import com.onebyte.backend.domain.character.entity.UserItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserItemRepository extends JpaRepository<UserItem, UserItemId> {

    /** 보유 아이템 전체 목록 */
    @Query("""
            select ui from UserItem ui join fetch ui.item i
            where ui.id.userId = :userId
            order by ui.acquiredAt desc, i.id asc
            """)
    List<UserItem> findOwned(@Param("userId") Long userId);

    /** 보유 아이템 목록 (슬롯 필터) */
    @Query("""
            select ui from UserItem ui join fetch ui.item i
            where ui.id.userId = :userId and i.slot = :slot
            order by ui.acquiredAt desc, i.id asc
            """)
    List<UserItem> findOwnedBySlot(@Param("userId") Long userId, @Param("slot") String slot);

    /** 요청한 아이템 중 사용자가 보유한 것만 조회 (장착 검증용) */
    @Query("""
            select ui from UserItem ui join fetch ui.item
            where ui.id.userId = :userId and ui.id.itemId in :itemIds
            """)
    List<UserItem> findOwnedByItemIds(@Param("userId") Long userId,
                                      @Param("itemIds") Collection<Long> itemIds);
}