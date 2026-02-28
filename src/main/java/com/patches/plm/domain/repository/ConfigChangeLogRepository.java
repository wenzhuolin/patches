package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.ConfigChangeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigChangeLogRepository extends JpaRepository<ConfigChangeLogEntity, Long> {
}
