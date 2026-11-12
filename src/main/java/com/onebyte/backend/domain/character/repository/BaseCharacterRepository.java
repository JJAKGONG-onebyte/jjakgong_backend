package com.onebyte.backend.domain.character.repository;

import com.onebyte.backend.domain.character.entity.BaseCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseCharacterRepository extends JpaRepository<BaseCharacter, Long> {
}
