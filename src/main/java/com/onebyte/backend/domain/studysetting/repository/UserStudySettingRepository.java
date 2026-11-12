package com.onebyte.backend.domain.studysetting.repository;

import com.onebyte.backend.domain.studysetting.entity.UserStudySetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStudySettingRepository extends JpaRepository<UserStudySetting, Long> {
}
