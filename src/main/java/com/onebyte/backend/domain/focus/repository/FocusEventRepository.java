package com.onebyte.backend.domain.focus.repository;

import com.onebyte.backend.domain.focus.entity.FocusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusEventRepository extends JpaRepository<FocusEvent, Long> {
}
