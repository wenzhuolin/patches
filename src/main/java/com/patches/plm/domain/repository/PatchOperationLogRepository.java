package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.PatchOperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatchOperationLogRepository extends JpaRepository<PatchOperationLogEntity, Long> {
    java.util.List<PatchOperationLogEntity> findByTenantIdAndBizTypeAndBizIdOrderByCreatedAtDesc(
            Long tenantId, String bizType, Long bizId
    );

    java.util.List<PatchOperationLogEntity> findByTenantIdAndBizTypeOrderByCreatedAtDesc(Long tenantId, String bizType);
}
