package com.onebyte.backend.domain.character.service;

import com.onebyte.backend.domain.character.dto.CharacterResponse;
import com.onebyte.backend.domain.character.dto.CharacterResponse.Customization;
import com.onebyte.backend.domain.character.dto.MyItemListResponse;
import com.onebyte.backend.domain.character.dto.SaveCharacterRequest;
import com.onebyte.backend.domain.character.entity.BaseCharacter;
import com.onebyte.backend.domain.character.entity.CharacterSlot;
import com.onebyte.backend.domain.character.entity.Item;
import com.onebyte.backend.domain.character.entity.UserCharacter;
import com.onebyte.backend.domain.character.entity.UserCharacterEquipment;
import com.onebyte.backend.domain.character.entity.UserItem;
import com.onebyte.backend.domain.character.repository.UserCharacterEquipmentRepository;
import com.onebyte.backend.domain.character.repository.UserCharacterRepository;
import com.onebyte.backend.domain.character.repository.UserItemRepository;
import com.onebyte.backend.domain.character.service.CharacterLevelPolicy.LevelInfo;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final UserCharacterRepository userCharacterRepository;
    private final UserCharacterEquipmentRepository equipmentRepository;
    private final UserItemRepository userItemRepository;
    private final StudyMinutesReader studyMinutesReader;
    private final CharacterLevelPolicy levelPolicy;

    /** 3-1. 내 캐릭터 조회 (캐릭터 종류 · 이름 · 레벨 · 색상 · 슬롯별 장착 현황) */
    @Transactional(readOnly = true)
    public CharacterResponse getMyCharacter(Long userId) {
        UserCharacter userCharacter = findUserCharacter(userId);
        BaseCharacter base = userCharacter.getUser().getCharacter();
        if (base == null) {
            throw new CustomException(ErrorCode.CHARACTER_NOT_FOUND);
        }

        LevelInfo level = levelPolicy.calculate(studyMinutesReader.readTotalMinutes(userId));

        return new CharacterResponse(
                base.getCharacterId(),
                base.getImageUrl(),
                userCharacter.getName(),
                level.level(),
                level.minutesToNextLevel(),
                level.levelProgressPercent(),
                userCharacter.getColor(),
                buildCustomizations(userId),
                userCharacter.getUpdatedAt());
    }

    /** 3-2. 캐릭터 저장(저장하기): 색상 변경 + 장착 현황 전체 교체. 목록에 없는 슬롯은 해제 처리 */
    @Transactional
    public void saveMyCharacter(Long userId, SaveCharacterRequest request) {
        if (!HEX_COLOR.matcher(request.color().trim()).matches()) {
            throw new CustomException(ErrorCode.INVALID_COLOR);
        }
        Map<CharacterSlot, Long> requested = parseRequestedSlots(request);
        Map<CharacterSlot, Item> toEquip = validateOwnedItems(userId, requested);

        // 검증을 모두 통과한 뒤에만 기존 장착 현황을 지운다 (벌크 삭제 후 영속성 컨텍스트가 비워지므로 그 뒤에 조회)
        equipmentRepository.deleteAllByUserId(userId);

        UserCharacter userCharacter = findUserCharacter(userId);
        userCharacter.changeColor(request.color().trim().toUpperCase());

        List<UserCharacterEquipment> equipments = new ArrayList<>();
        toEquip.forEach((slot, item) ->
                equipments.add(UserCharacterEquipment.of(userCharacter, slot.name(), item)));
        equipmentRepository.saveAll(equipments);
    }

    /** 3-3. 보유 아이템 목록 (slot이 없으면 전체) */
    @Transactional(readOnly = true)
    public MyItemListResponse getMyItems(Long userId, String slot) {
        if (slot == null || slot.isBlank()) {
            return MyItemListResponse.from(userItemRepository.findOwned(userId));
        }
        CharacterSlot characterSlot = CharacterSlot.from(slot)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_SLOT));
        return MyItemListResponse.from(userItemRepository.findOwnedBySlot(userId, characterSlot.name()));
    }

    private UserCharacter findUserCharacter(Long userId) {
        return userCharacterRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
    }

    /** 슬롯 5개를 항상 같은 순서로 내려준다. 장착하지 않은 슬롯은 null */
    private List<Customization> buildCustomizations(Long userId) {
        Map<String, Item> equipped = equipmentRepository.findAllWithItem(userId).stream()
                .collect(Collectors.toMap(e -> e.getId().getSlot(), UserCharacterEquipment::getItem));

        List<Customization> result = new ArrayList<>();
        for (CharacterSlot slot : CharacterSlot.values()) {
            Item item = equipped.get(slot.name());
            result.add(item == null
                    ? new Customization(slot.name(), null, null)
                    : new Customization(slot.name(), item.getId(), item.getImageUrl()));
        }
        return result;
    }

    /** 요청의 슬롯 값 검증 (존재하지 않는 슬롯, 같은 슬롯 중복은 INVALID_SLOT) */
    private Map<CharacterSlot, Long> parseRequestedSlots(SaveCharacterRequest request) {
        Map<CharacterSlot, Long> requested = new EnumMap<>(CharacterSlot.class);
        for (SaveCharacterRequest.Customization c : request.customizations()) {
            CharacterSlot slot = CharacterSlot.from(c.slot())
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_SLOT));
            if (requested.containsKey(slot)) {
                throw new CustomException(ErrorCode.INVALID_SLOT, "같은 슬롯이 중복되었습니다: " + slot.name());
            }
            requested.put(slot, c.itemId());
        }
        return requested;
    }

    /** 장착하려는 아이템이 모두 보유 중이고 슬롯과 맞는지 확인 (itemId가 null인 슬롯은 해제라 제외) */
    private Map<CharacterSlot, Item> validateOwnedItems(Long userId, Map<CharacterSlot, Long> requested) {
        Set<Long> itemIds = requested.values().stream()
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return new EnumMap<>(CharacterSlot.class);
        }

        Map<Long, Item> owned = userItemRepository.findOwnedByItemIds(userId, itemIds).stream()
                .map(UserItem::getItem)
                .collect(Collectors.toMap(Item::getId, i -> i));

        Map<CharacterSlot, Item> result = new EnumMap<>(CharacterSlot.class);
        for (Map.Entry<CharacterSlot, Long> entry : requested.entrySet()) {
            Long itemId = entry.getValue();
            if (itemId == null) {
                continue;
            }
            Item item = owned.get(itemId);
            if (item == null) {
                throw new CustomException(ErrorCode.ITEM_NOT_OWNED);
            }
            if (!entry.getKey().name().equals(item.getSlot())) {
                throw new CustomException(ErrorCode.INVALID_SLOT);
            }
            result.put(entry.getKey(), item);
        }
        return result;
    }
}