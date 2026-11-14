package com.onebyte.backend.domain.character.service;

import com.onebyte.backend.domain.character.dto.BaseCharacterListResponse;
import com.onebyte.backend.domain.character.repository.BaseCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BaseCharacterService {

    private final BaseCharacterRepository baseCharacterRepository;

    @Transactional(readOnly = true)
    public BaseCharacterListResponse getBaseCharacters() {
        return BaseCharacterListResponse.from(baseCharacterRepository.findAll(Sort.by("characterId")));
    }
}
