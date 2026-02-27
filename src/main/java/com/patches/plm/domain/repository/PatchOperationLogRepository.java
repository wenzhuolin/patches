package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.PatchOperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatchOperationLogRepository extends JpaRepository<PatchOperationLogEntity, Long> {
}
