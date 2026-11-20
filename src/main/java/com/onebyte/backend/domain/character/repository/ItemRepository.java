package com.onebyte.backend.domain.character.repository;

import com.onebyte.backend.domain.character.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}