package com.onebyte.backend.domain.character.repository;

import com.onebyte.backend.domain.character.entity.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
}
