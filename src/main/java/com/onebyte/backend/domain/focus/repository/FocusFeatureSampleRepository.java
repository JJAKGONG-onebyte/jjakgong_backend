package com.onebyte.backend.domain.focus.repository;

import com.onebyte.backend.domain.focus.entity.FocusFeatureSample;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusFeatureSampleRepository extends JpaRepository<FocusFeatureSample, Long> {
}
